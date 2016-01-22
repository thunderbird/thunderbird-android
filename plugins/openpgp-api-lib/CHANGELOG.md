# Version history

## Version 10
  * Retrieve whole public key via ACTION_GET_KEY

## Version 9
  * AIDL Service has been changed from IOpenPgpService.aidl to IOpenPgpService2.aidl  
    This fixes truncated data streams (thanks to 'mgeier63').
  * Fix for OpenPgpKeyPreference: Properly execute pending user interactions
  * Charset moved to OpenPgpMetadata

## Version 8
  * OpenPgpSignatureResult:  
    method getStatus() renamed to getResult()  
    constants have been renamed for clarity  
    new constants: RESULT_NO_SIGNATURE, RESULT_INVALID_INSECURE  
    isSignatureOnly() has been deprecated
  * RESULT_TYPES have been removed
  * new OpenPgpDecryptionResult returned via RESULT_DECRYPTION
  * OpenPgpSignatureResult and OpenPgpDecryptionResult are never null, they are always returned.

## Version 7
  * Deprecation of ACCOUNT_NAME, please use ACTION_GET_SIGN_KEY_ID to get key id
  * Introduce EXTRA_SIGN_KEY_ID
  * New extra for ACTION_ENCRYPT and ACTION_SIGN_AND_ENCRYPT: EXTRA_ENABLE_COMPRESSION (default to true)
  * Return PendingIntent to view key for signatures
  * New result for ACTION_DECRYPT_VERIFY: RESULT_TYPE
  * New ACTION_GET_SIGN_KEY_ID
  * EXTRA_PASSPHRASE changed from String to char[]

## Version 6
  * Deprecate ACTION_SIGN
  * Introduce ACTION_CLEARTEXT_SIGN and ACTION_DETACHED_SIGN
  * New extra for ACTION_DETACHED_SIGN: EXTRA_DETACHED_SIGNATURE
  * New result for ACTION_DECRYPT_VERIFY: RESULT_DETACHED_SIGNATURE
  * New result for ACTION_DECRYPT_VERIFY: RESULT_CHARSET

## Version 5
  * OpenPgpSignatureResult: new consts RESULT_INVALID_KEY_REVOKED and RESULT_INVALID_KEY_EXPIRED
  * OpenPgpSignatureResult: ArrayList<String> userIds

## Version 4
  * No changes to existing methods -> backward compatible
  * Introduction of ACTION_DECRYPT_METADATA, RESULT_METADATA, EXTRA_ORIGINAL_FILENAME, and OpenPgpMetadata parcel
  * Introduction of internal NFC extras: EXTRA_NFC_SIGNED_HASH, EXTRA_NFC_SIG_CREATION_TIMESTAMP

## Version 3
  * First public stable version