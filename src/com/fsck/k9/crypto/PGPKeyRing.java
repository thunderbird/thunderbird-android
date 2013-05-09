package com.fsck.k9.crypto;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.widget.Toast;
//import android.util.Log;

import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.imaeses.squeaky.R;

/**
 * PGP KeyRing integration. (9 May 2013).
 * Also modified: crypto/CryptoProvider.java, activity/setup/AccountSettings.java, res/values/strings.xml, and res/values/arrays.xml
 *
 * @author Adam Wasserman
 *
 */
public class PGPKeyRing extends CryptoProvider {

    public static final String NAME = "pgpkeyring";
    
    public static final String PACKAGE_PAID = "com.imaeses.keyring";
    public static final String PACKAGE_TRIAL = "com.imaeses.keyring.trial";
    public static final String APP_NAME_PAID = "KeyRing";
    public static final int VERSION_REQUIRED_MIN = 22;

    public static final String AUTHORITY_PAID = "com.imaeses.KeyRing";
    public static final String AUTHORITY_TRIAL = "com.imaeses.trial.KeyRing";
    
    public static final String PROVIDER_KEYID = "keyid";
    public static final String PROVIDER_USERID = "userid";
    public static final String PROVIDER_IDENTITY = "identity";
    
    public static final int DECRYPT_MESSAGE = 1;
    public static final int ENCRYPT_MESSAGE = 2;
    public static final int SELECT_PUBLIC_KEYS = 3;
    public static final int SELECT_SECRET_KEY = 4;

    private Uri uriSelectPrivateSigningKey;
    private Uri uriSelectPublicSigningKey;
    private Uri uriSelectPrivateEncKey;
    private Uri uriSelectPublicEncKey;
    private Uri uriSelectPublicKeysByEmail;
    private Uri uriSelectPrivateKeysByEmail;
    private Uri uriSelectPrimaryUserIdByKeyid;
    private boolean isTrialVersion;
    
    public static final String EXTRAS_MSG = "msg";
    public static final String EXTRAS_ENCRYPTION_KEYIDS = "keys.enc";
    public static final String EXTRAS_SIGNATURE_KEYID = "sig.key";
    public static final String EXTRAS_SIGNATURE_SUCCESS = "sig.success";
    public static final String EXTRAS_SIGNATURE_IDENTITY = "sig.identity";
    public static final String EXTRAS_EMAIL_ADDRESSES = "email.addresses";
    public static final String EXTRAS_PRESELECTED = "keys.preselected";
    public static final String EXTRAS_SELECTION_MULTI = "selection.mode.multi";
    public static final String EXTRAS_CHOSEN_KEYIDS = "chosen.keyids";
    public static final String EXTRAS_CHOSEN_KEY = "chosen.key";
    public static final String EXTRAS_SIGNATURE_UNKNOWN = "sig.unknown";
    public static final String EXTRAS_SHOW_KEYID_IN_SINGLE_SELECTION = "show.keyid.single.selection";
    
    public static class PGPKeyRingIntent {
        
        public static final String ENCRYPT_MSG_AND_RETURN = "com.imaeses.keyring.ENCRYPT_MSG_AND_RETURN";
        public static final String DECRYPT_MSG_AND_RETURN = "com.imaeses.keyring.DECRYPT_MSG_AND_RETURN";
        
    }
    
    public static Pattern PGP_MESSAGE = 
            Pattern.compile( ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL );
    
    public static Pattern PGP_SIGNED_MESSAGE =
            Pattern.compile( ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*", Pattern.DOTALL );
    
    public static PGPKeyRing createInstance() {
        return new PGPKeyRing();
    }
    
    public PGPKeyRing() {
        
        isTrialVersion = true;
        setContentUris();
        
    }
    
    /**
     * Check whether PGPKeyRing is installed and at a high enough version.
     *
     * @param context
     * @return whether a suitable version of PGPKeyRing was found
     */
    @Override
    public boolean isAvailable( Context context ) {
              
        boolean isSuitable = false;
                  
        PackageInfo pi = null;
        PackageManager packageManager = context.getPackageManager();
        
        try {
           
            pi = packageManager.getPackageInfo( PACKAGE_PAID, 0 ); 
            
            isTrialVersion = false;
            setContentUris();
                
        } catch( NameNotFoundException e ) {
                
            try {         
                pi = packageManager.getPackageInfo( PACKAGE_TRIAL, 0 );
            } catch( NameNotFoundException f ) {
            }
                
        }
            
        if( pi != null && pi.versionCode >= VERSION_REQUIRED_MIN ) {
            isSuitable = true;
        } else {
            Toast.makeText( context, R.string.error_pgpkeyring_version_not_supported, Toast.LENGTH_SHORT ).show(); 
        }

        return isSuitable;
        
    }
    
    /**
     * Select the key for generating a signature.
     *
     * @param activity
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectSecretKey( Activity activity, PgpData pgpData ) {
        
        boolean success = false;
        
        try {
         
            Intent i = new Intent( Intent.ACTION_PICK );
            i.putExtra( EXTRAS_SHOW_KEYID_IN_SINGLE_SELECTION, true );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setData( uriSelectPrivateSigningKey );
            
            activity.startActivityForResult( i, SELECT_SECRET_KEY );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
            Toast.makeText( activity, R.string.error_activity_not_found, Toast.LENGTH_SHORT ).show();
        }
        
        return success;
        
    }
    
    /**
     * Select keys for use in encrypting.
     *
     * @param activity
     * @param emails The emails that should be used for preselection.
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean selectEncryptionKeys( Activity activity, String emails, PgpData pgpData ) {
        
        boolean success = false;
        
        try {
         
            Intent i = new Intent( Intent.ACTION_PICK );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setData( uriSelectPublicEncKey );
            i.putExtra( EXTRAS_SELECTION_MULTI, true );
            
            long[] preselected = null;
                    
            if( !pgpData.hasEncryptionKeys() ) {
               
                if( pgpData.hasSignatureKey() ) {        
                    preselected = new long[] { pgpData.getSignatureKeyId() };
                }
                
                if( emails != null && emails.length() > 0 ) {
                    i.putExtra( EXTRAS_EMAIL_ADDRESSES, emails.split( "," ) );
                }
                
            } else {
                preselected = pgpData.getEncryptionKeys();
            }
            
            if( preselected != null ) {
                i.putExtra( EXTRAS_PRESELECTED, preselected );
            }
            
            activity.startActivityForResult( i, SELECT_PUBLIC_KEYS );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
            Toast.makeText( activity, R.string.error_activity_not_found, Toast.LENGTH_SHORT ).show();
        }
        
        return success;
        
    }
    
    /**
     * Get key ids in secret key rings based on a given email. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getSecretKeyIdsFromEmail( Context context, String email ) {
    
        String[] projection = new String[] { PROVIDER_KEYID };
        
        long[] keyids = null;
        Uri.Builder builder = uriSelectPrivateKeysByEmail.buildUpon();
        builder.appendPath( email );
        Uri uri = builder.build();
               
        Cursor c = null;
        try {
                
            c = context.getContentResolver().query( uri, projection, null, null, null );
            if( c != null ) {
                
                keyids = new long[ c.getCount() ];
            
                while( c.moveToNext() ) {
                    
                    String keyId = c.getString( 0 );
                    keyids[ c.getPosition() ] = new BigInteger( keyId, 16 ).longValue();
                    
                }
                
            }
            
        } catch( SecurityException e ) {
            Toast.makeText( context, context.getResources().getString( R.string.insufficient_pgpkeyring_permissions ), Toast.LENGTH_SHORT ).show();    
        } finally {
            if( c != null ) {
                c.close();
            }
        }
        
        return keyids;
            
    }
    
    /**
     * Get key ids in public key rings based on a given email. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return key ids
     */
    @Override
    public long[] getPublicKeyIdsFromEmail( Context context, String email ) {
    
        String[] projection = new String[] { PROVIDER_KEYID };
        
        long[] keyids = null;
        Uri.Builder builder = uriSelectPublicKeysByEmail.buildUpon();
        builder.appendPath( email );
        Uri uri = builder.build();
               
        Cursor c = null;
        try {
                
            c = context.getContentResolver().query( uri, projection, null, null, null );
            if( c != null ) {
                
                keyids = new long[ c.getCount() ];
            
                while( c.moveToNext() ) {
                    
                    String keyId = c.getString( 0 );
                    keyids[ c.getPosition() ] = new BigInteger( keyId, 16 ).longValue();
                    
                }
                
            }
                
        } catch( SecurityException e ) {
            Toast.makeText( context, context.getResources().getString( R.string.insufficient_pgpkeyring_permissions ), Toast.LENGTH_SHORT ).show();    
        } finally {
            if( c != null ) {
                c.close();
            }
        }
        
        return keyids;
        
    }
    
    /**
     * Find out if a given email has a secret key. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return true if there is a secret key for this email.
     */
    @Override
    public boolean hasSecretKeyForEmail( Context context, String email ) {
        
        long[] keyids = getSecretKeyIdsFromEmail( context, email );
        return keyids != null && keyids.length > 0;
        
    }
    
    /**
     * Find out if a given email has a public key. For encrypting keys.
     *
     * @param context
     * @param email The email in question.
     * @return true if there is a public key for this email.
     */
    @Override
    public boolean hasPublicKeyForEmail( Context context, String email ) {
        
        long[] keyids = getPublicKeyIdsFromEmail( context, email );
        return keyids != null && keyids.length > 0;
        
    } 
    
    /**
     * Get the user id based on the key id.
     *
     * @param context
     * @param keyId
     * @return user id
     */
    @Override
    public String getUserId( Context context, long keyId ) {
    
        String[] projection = new String[] { PROVIDER_USERID };
        
        Uri.Builder builder = uriSelectPrimaryUserIdByKeyid.buildUpon();
        builder.appendEncodedPath( Long.toHexString( keyId ) );
        Uri uri = builder.build();
        
        Cursor c = null;
        String userId = null;
        try {
            
            c = context.getContentResolver().query( uri, projection, null, null, null );
        
            if( c != null && c.moveToFirst() ) {
                userId = c.getString( 0 );
            }
            
        } finally {
            if( c != null ) {
                c.close();
            }
        }
        
        return userId;
        
    }
    
    /**
     * Start the encrypt activity.
     *
     * @param activity
     * @param data
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean encrypt( Activity activity, String data, PgpData pgpData ) {
        
        boolean success = false;
        
        Intent i = new Intent( PGPKeyRingIntent.ENCRYPT_MSG_AND_RETURN );
        i.addCategory( Intent.CATEGORY_DEFAULT );
        i.setType( "text/plain" );
        i.putExtra( EXTRAS_MSG, data);
        i.putExtra( EXTRAS_ENCRYPTION_KEYIDS, pgpData.getEncryptionKeys() );
        i.putExtra( EXTRAS_SIGNATURE_KEYID, pgpData.getSignatureKeyId() );
        
        try {
            
            activity.startActivityForResult( i, ENCRYPT_MESSAGE );
            success = true;
            
        } catch( ActivityNotFoundException e ) {
            Toast.makeText( activity, R.string.error_activity_not_found, Toast.LENGTH_SHORT ).show();
        }
        
        return success;
        
    }
    
    /**
     * Start the decrypt activity.
     *
     * @param fragment
     * @param data
     * @param pgpData
     * @return success or failure
     */
    @Override
    public boolean decrypt( Fragment fragment, String data, PgpData pgpData ) {
        
        boolean success = false;
        
        if( data != null && data.length() > 0 ) {
            
            Intent i = new Intent( PGPKeyRingIntent.DECRYPT_MSG_AND_RETURN );
            i.addCategory( Intent.CATEGORY_DEFAULT );
            i.setType( "text/plain" );
            i.putExtra( EXTRAS_MSG, data );
            
            try {
            
                fragment.startActivityForResult( i, DECRYPT_MESSAGE );
                success = true;
            
            } catch( ActivityNotFoundException e ) {
                Toast.makeText( fragment.getActivity(), R.string.error_activity_not_found, Toast.LENGTH_SHORT ).show();
            }
            
        }
        
        return success;
        
    }
    
    /**
     * Handle the activity results that concern us.
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    @Override
    public boolean onActivityResult( Activity activity, int requestCode, int resultCode, Intent data, PgpData pgpData ) {
        
        switch (requestCode) {
        
        case SELECT_SECRET_KEY:
            
            if( resultCode != Activity.RESULT_OK || data == null ) {
                break;
            }
            
            ContentValues values = ( ContentValues )data.getParcelableExtra( EXTRAS_CHOSEN_KEY );
            
            pgpData.setSignatureKeyId( new BigInteger( values.getAsString( PROVIDER_KEYID ), 16 ).longValue() );
            pgpData.setSignatureUserId( values.getAsString( PROVIDER_IDENTITY ) );
            
            ( ( MessageCompose )activity ).updateEncryptLayout();
            
            break;

        case SELECT_PUBLIC_KEYS:
            
            if( resultCode != Activity.RESULT_OK || data == null ) {            
                pgpData.setEncryptionKeys( null );
            } else {
                pgpData.setEncryptionKeys( data.getLongArrayExtra( EXTRAS_CHOSEN_KEYIDS ) );
            }
            
            ( ( MessageCompose )activity ).onEncryptionKeySelectionDone();
            
            break;

        case ENCRYPT_MESSAGE:
            
            if( resultCode != Activity.RESULT_OK || data == null ) {       
                pgpData.setEncryptionKeys( null );
            } else {   
                pgpData.setEncryptedData( data.getStringExtra( EXTRAS_MSG ) );
            }
            
            ( ( MessageCompose )activity ).onEncryptDone();
            
            break;

        default:
            return false;
            
        }

        return true;
    }

    @Override
    public boolean onDecryptActivityResult( CryptoDecryptCallback callback, int requestCode, int resultCode, Intent data, PgpData pgpData ) {

        switch( requestCode ) {
        
        case DECRYPT_MESSAGE:
            
            if( resultCode == Activity.RESULT_OK && data != null ) {

                pgpData.setSignatureUserId( data.getStringExtra( EXTRAS_SIGNATURE_IDENTITY ) );
                pgpData.setSignatureKeyId( data.getLongExtra( EXTRAS_SIGNATURE_KEYID, 0 ) );
                pgpData.setSignatureSuccess( data.getBooleanExtra( EXTRAS_SIGNATURE_SUCCESS, false ) );
                pgpData.setSignatureUnknown( data.getBooleanExtra( EXTRAS_SIGNATURE_UNKNOWN, false ) );
    
                pgpData.setDecryptedData( data.getStringExtra( EXTRAS_MSG ) );
                
                //Log.w( NAME, "signature identity: " + pgpData.getSignatureUserId() + ", keyid: " + pgpData.getSignatureKeyId() + ", success: " + pgpData.getSignatureSuccess() + ", sigs unknown: " + pgpData.getSignatureUnknown() );
                callback.onDecryptDone( pgpData );
                
            }

            break;
            
        default:
            return false;

        }

        return true;
    }
    
    @Override
    public boolean isEncrypted( Message message ) {
              
        String data = null;
        try {
            
            Part part = MimeUtility.findFirstPartByMimeType( message, "text/plain" );
            if( part == null ) {
                part = MimeUtility.findFirstPartByMimeType( message, "text/html" );
            }
            
            if( part != null ) {
                data = MimeUtility.getTextFromPart( part );
            }
            
        } catch( MessagingException e ) {
            return false;
        }

        if( data == null ) {
            return false;
        }

        Matcher matcher = PGP_MESSAGE.matcher( data );
       
        return matcher.matches();
        
    }

    @Override
    public boolean isSigned(Message message) {
             
        String data = null;
        try {
            
            Part part = MimeUtility.findFirstPartByMimeType( message, "text/plain" );
            if( part == null ) {
                part = MimeUtility.findFirstPartByMimeType( message, "text/html" );
            }
            
            if( part != null ) {
                data = MimeUtility.getTextFromPart( part );
            }
            
        } catch( MessagingException e ) {
            return false;
        }

        if( data == null ) {
            return false;
        }

        Matcher matcher = PGP_SIGNED_MESSAGE.matcher(data);
        return matcher.matches();
        
    }
    
    public String getName() {
        return NAME;
    }
    
    /**
     * Test the PGP KeyRing installation.
     *
     * @return success or failure
     */
    @Override
    public boolean test( Context context ) {
        
        if ( !isAvailable( context ) ) {
            return false;
        }

        try {
            
            Uri.Builder builder = uriSelectPrimaryUserIdByKeyid.buildUpon();
            builder.appendEncodedPath( "1122334455667788" );
            Uri uri = builder.build();
            
            Cursor c = context.getContentResolver().query( uri, new String[] { "user_id" }, null, null, null );
            if( c != null ) {
                c.close();
            }
            
        } catch (SecurityException e) {
            Toast.makeText( context, context.getResources().getString( R.string.insufficient_pgpkeyring_permissions ), Toast.LENGTH_LONG ).show();
        }

        return true;
    }
   
    private void setContentUris() {
        
        if( isTrialVersion ) {
            
            uriSelectPublicSigningKey = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/public/sign" );
            uriSelectPrivateSigningKey = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/private/sign" );
            uriSelectPublicEncKey = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/public/encrypt" );
            uriSelectPrivateEncKey = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/private/encrypt" );
            uriSelectPublicKeysByEmail = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/public/email" );
            uriSelectPrivateKeysByEmail = Uri.parse( "content://" + AUTHORITY_TRIAL + "/keys/private/email" );
            uriSelectPrimaryUserIdByKeyid = Uri.parse( "content://" + AUTHORITY_TRIAL + "/userid/keyid/subkey" );
            
        } else {

            uriSelectPublicSigningKey = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/public/sign" );
            uriSelectPrivateSigningKey = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/private/sign" );
            uriSelectPublicEncKey = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/public/encrypt" );
            uriSelectPrivateEncKey = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/private/encrypt" );
            uriSelectPublicKeysByEmail = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/public/email" );
            uriSelectPrivateKeysByEmail = Uri.parse( "content://" + AUTHORITY_PAID + "/keys/private/email" );
            uriSelectPrimaryUserIdByKeyid = Uri.parse( "content://" + AUTHORITY_PAID + "/userid/keyid/subkey" );
            
        }
        
    }
    
}
