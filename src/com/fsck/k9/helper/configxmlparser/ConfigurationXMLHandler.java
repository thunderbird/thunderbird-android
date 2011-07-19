/*
 * XMLDescriptionHandler.java
 * author: dzan
 *
 * Parses XML files containing information to configure email accounts.
 * These files are writtin according to the Mozilla defined XML format.
 *
 * This class is not made to verify the correctness of XML input! It assumes correct input!
 * However it does alert of incorrect xml in some cases when there was too much extra work required to do so.
 * Example: Missing 'type' attribute in server tags are noted.
 * Example: Unallowed nesting of some tags will crash the parser!
 *
 * Information urls:
 *  http://viewvc.svn.mozilla.org/vc/mozillamessaging.com/sites/ispdb.mozillamessaging.com/trunk/ispdb/tests/relaxng_schema.1.1.xml?revision=74797&view=markup
 *  https://wiki.mozilla.org/Thunderbird:Autoconfiguration:ConfigFileFormat
 *  https://developer.mozilla.org/en/Thunderbird/Autoconfiguration/FileFormat/HowTo
 *
 *  ( https://developer.mozilla.org/en/Thunderbird/Autoconfiguration )
 */

/*
 TODO: add support for more then one 'enable' tag set
 TODO: there is no support for stand alone 'instruction' tags in the relax-ng scheme, it is there in examples, figure this out
 TODO: implement the foreseen server-check method
*/

    /*
        NOTE: For now I ignore the clientDescription & clientDescriptionUpdate
        since I have no clue what they are for and doubt they will be useful for k9.
     */

package com.fsck.k9.helper.configxmlparser;

// Sax stuff
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

// Types
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.AuthenticationType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.SocketType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.RestrictionType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.ServerType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.Server;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.IncomingServerPOP3;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.OutgoingServerSMTP;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.InputField;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.InformationBlock;

// Other
import java.util.HashMap;
import java.util.Map;

public class ConfigurationXMLHandler extends DefaultHandler {

    /*********************************************************************************
                           Data we need to parse
     *********************************************************************************/
    /*
        Enums replacing strings to recognise tags and attributes
        Switching on enums is cleaner ( and I'm pretty sure faster ) then a huge if/elseif/else with String.equals(..)

        NOTE: the enum value must be the EXACT upper-case version of the tag/attribute name.
     */
    private enum TAG{
        // main structure tags
        CLIENTCONFIG, CLIENTCONFIGUPDATE, EMAILPROVIDER, INCOMINGSERVER, OUTGOINGSERVER,

        // email provider extra information
        DOMAIN, DISPLAYNAME, DISPLAYSHORTNAME, ENABLE, INSTRUCTION, IDENTITY, INPUTFIELD, DOCUMENTATION, DESCR,

        // server settings
        HOSTNAME, PORT, SOCKETTYPE, USERNAME, AUTHENTICATION,

        // pop3 options
        POP3, LEAVEMESSAGESONSERVER, DOWNLOADONBIFF, DAYSTOLEAVEMESSAGESONSERVER, CHECKINTERVAL,

        // outgoing server settings
        RESTRICTION, ADDTHISSERVER, USEGLOBALPREFERREDSERVER,

        // meta
        NO_VALUE, WRONG_TAG;

        public static TAG toTag(String str){
            try{
                return valueOf(str.toUpperCase());
            }catch (IllegalArgumentException ex){
                return WRONG_TAG;
            }catch (NullPointerException ex){
                return NO_VALUE;
            }
        }
    }

    // I like enum values in all uppercase, using lowercase would avoid all this, although the method described here
    // could be made more generic then using lowercase because it is in this case ( confusing :D )
    // assuming here attributes are always all lowercase ( as noticed in the relaxng scheme )
    // if camelCase or others would ever be used, just add a constructor with a String argument and
    // change the getXMLStringVersion to return that value instead of all lowercase one.
    private enum ATTRIBUTE{
        // main structure attributes
        ID, TYPE,

        // email provider extra information
        VISITURL, INSTRUCTION, DESCR, LANG, URL, KEY, LABEL,

        // server settings
        POP3, IMAP, SMTP,

        // pop3 options
        MINUTES,

        // meta
        NO_VALUE, WRONG_TAG;

        public static ATTRIBUTE toAttribute(String str){
            try{
                return valueOf(str.toUpperCase());
            }catch (IllegalArgumentException ex){
                return WRONG_TAG;
            }catch (NullPointerException ex){
                return NO_VALUE;
            }
        }

        public String getXMLStringVersion(){ return toString().toLowerCase(); }
    }

    /*
        Mappings
     */
    public static Map<String, AuthenticationType> authenticationTypeMap = new HashMap<String, AuthenticationType>();
    public static Map<String, SocketType> socketTypeMap = new HashMap<String, SocketType>();
    //public static Map<String, RestrictionType> restrictionTypeMap = new HashMap<String, RestrictionType>();
    static{
        // all known authentication types
        authenticationTypeMap.put("plain",AuthenticationType.plain);
        authenticationTypeMap.put("password-cleartext",AuthenticationType.plain);
        authenticationTypeMap.put("secure",AuthenticationType.secure);
        authenticationTypeMap.put("password-encrypted",AuthenticationType.secure);
        authenticationTypeMap.put("NTLM",AuthenticationType.NTLM);
        authenticationTypeMap.put("GSSAPI",AuthenticationType.GSSAPI);
        authenticationTypeMap.put("client-IP-address",AuthenticationType.clientIPaddress);
        authenticationTypeMap.put("TLS-client-cert",AuthenticationType.TLSclientcert);
        authenticationTypeMap.put("none",AuthenticationType.none);

        // known socket types
        socketTypeMap.put("plain",SocketType.plain);
        socketTypeMap.put("SSL",SocketType.SSL);
        socketTypeMap.put("STARTTLS", SocketType.STARTTLS);

        // restriction types
        // for now this is unused but this allows for easier future expansion
        //restrictionTypeMap.put("clientIPAddress", RestrictionType.clientIPAddress);
    }

    /*
        These flags are used so the 'characters(..)' method executes the correct code.
        They are used to make that method 'context-aware'.
     */
	private boolean mIsDomain;
	private boolean mIsDisplayName;
	private boolean mIsDisplayShortName;

	private boolean mIsHostname;
	private boolean mIsPort;
	private boolean mIsSocketType;
	private boolean mIsUsername;
	private boolean mIsAuthentication;

	private boolean mIsLeaveMessagesOnServer;
	private boolean mIsDownloadOnBiff;
	private boolean mIsDaysToLeaveMessagesOnServer;

	private boolean mIsRestriction;
	private boolean mIsAddThisServer;
	private boolean mIsUseGlobalPreferredServer;

	private boolean mIsInputField;
	private boolean mIsEnable;
    private boolean mIsInInformationString;

	private boolean mIsDescription;
	private boolean mIsDocumentation;
	private boolean mIsDescriptionInDoc;

    /*********************************************************************************
                          Parsing routine
     ********************************************************************************/
    // Main object we are filling
    private AutoconfigInfo mAutoconfigInfo;

    // Helper objects during parsing
    private Server mServerInProgress;
    private InputField mInputFieldInProgress;
    private InformationBlock mInformationBlockInProgress;
    private AutoconfigInfo.ParcelableMutablePair<String, String> mInformationStringInProgress;

    // Other stuff
    private Locator mLocator;

    // Getter for the data after it's parsed
    public AutoconfigInfo getAutoconfigInfo(){ return mAutoconfigInfo; }

    /*
        Methods overwritten from DefaultHandler
     */
    @Override
    public void setDocumentLocator(Locator locator){ this.mLocator = locator; }

    @Override
	public void startDocument() throws SAXException { mAutoconfigInfo = new AutoconfigInfo();}

	@Override
	public void endDocument() throws SAXException {
        /*
            Adding some checks here too to see if we have useable data
            TODO: add more
         */
        if( !(mAutoconfigInfo.outgoingServer.size() > 0 || mAutoconfigInfo.incomingServer.size() > 0))
            throw new SAXException("Unusable server data, not at least one incoming and outgoing server found.");
    }

    @Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
        switch( TAG.toTag(localName) ){
            case INCOMINGSERVER:
            case OUTGOINGSERVER:
                validateServer(mServerInProgress);
                // we can use this to check for illegal nesting of server tags in the startElement method
                mServerInProgress = null;
                break;
            case INPUTFIELD: mInputFieldInProgress = null; break;
            case ENABLE:
                mInformationBlockInProgress = null;
                mIsEnable = false;
                if( mInformationStringInProgress != null )
                    throw new SAXParseException("Unexpected ending of Enable-tag. Children were not closed correctly.",mLocator);
                break;
            case DOCUMENTATION:
            case INSTRUCTION:
                if( !mIsEnable ){
                    if( mInformationStringInProgress != null )
                        throw new SAXParseException("Unexpected ending of Instruction-tag. Children were not closed correctly.",mLocator);
                    mInformationBlockInProgress = null;
                }else{ mInformationStringInProgress = null; }
                break;
            case DESCR: mInformationStringInProgress = null;
                break;
            default: return;
        }
    }

    /*
        This double checks some basic invariants of the Server classes.
        Dealing with unpredictable input like this we better make sure we don't crash k-9.
     */
    private void validateServer(Server mServerInProgress) throws SAXException{
        // TODO: check if basic information is provided
    }

    /*
        Place where all the magic happens. The real parsing.
     */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

        // Recognise the tag
        switch ( TAG.toTag(localName) ){

            // Email Provider basic extra info
            case CLIENTCONFIG:  break; // ignore this for now
            case EMAILPROVIDER:
                mAutoconfigInfo.id = attributes.getValue(ATTRIBUTE.ID.getXMLStringVersion());
                break;
            case DOMAIN:            mIsDomain = true;           break;
            case DISPLAYNAME:       mIsDisplayName = true;      break;
            case DISPLAYSHORTNAME:  mIsDisplayShortName = true; break;

            // Enter incoming or outgoing server
            case INCOMINGSERVER:
            case OUTGOINGSERVER:
                if ( mServerInProgress != null )
                    throw new SAXParseException("Nested server-tags. This is not allowed!", mLocator);
                String type = attributes.getValue(ATTRIBUTE.TYPE.getXMLStringVersion());
                if( type != null ){
                    mServerInProgress = ServerType.toType(type).getServerObject(null);
                    mAutoconfigInfo.incomingServer.add(mServerInProgress);
                }else{ // this should never happen, this file is not formed correctly ( check the relaxng scheme )
                    throw new SAXParseException("Incoming|Outgoing-Server tag has no type attribute!", mLocator);
                }break;

            // Server stuff
            case HOSTNAME:          mIsHostname = true;         break;
            case PORT:              mIsPort = true;             break;
            case SOCKETTYPE:        mIsSocketType = true;       break;
            case USERNAME:          mIsUsername = true;         break;
            case AUTHENTICATION:    mIsAuthentication = true;   break;

            // POP3 extra options
            case POP3:
                if( mServerInProgress == null || mServerInProgress.type != ServerType.POP3 )
                    throw new SAXParseException
                            ("Illegal POP3 tag. It can only be nested in an incomingServer tag of type POP3", mLocator);
                break;
            case LEAVEMESSAGESONSERVER:
            case DOWNLOADONBIFF:
            case DAYSTOLEAVEMESSAGESONSERVER:
            case CHECKINTERVAL:
                if( mServerInProgress == null || mServerInProgress.type != ServerType.POP3 )
                    throw new SAXParseException
                            ("Illegal POP3 extra-settings tag. " +
                             "It can only be nested in an incomingServer tag of type POP3", mLocator);
                switch(TAG.toTag(localName)){
                case LEAVEMESSAGESONSERVER:         mIsLeaveMessagesOnServer = true; break;
                case DOWNLOADONBIFF:                mIsDownloadOnBiff = true; break;
                case DAYSTOLEAVEMESSAGESONSERVER:   mIsDaysToLeaveMessagesOnServer = true; break;
                case CHECKINTERVAL:
                    try{
                        ((IncomingServerPOP3)mServerInProgress).checkInterval =
                                Integer.parseInt(attributes.getValue(ATTRIBUTE.MINUTES.getXMLStringVersion()));
                    }catch(NumberFormatException ex){
                        throw new SAXParseException("Value of the minutes attribute was not an integer!",mLocator);
                    }
                    break;
                }
                break;

            // Outgoing extras
            case RESTRICTION:
            case ADDTHISSERVER:
            case USEGLOBALPREFERREDSERVER:
                if( mServerInProgress == null || mServerInProgress.type != ServerType.SMTP )
                    throw new SAXParseException
                            ("Illegal outgoingServer extra-settings tag. " +
                             "These can only be nested in an outgoingServer tag.", mLocator);
                switch(TAG.toTag(localName)){
                    case RESTRICTION:               mIsRestriction = true; break;
                    case ADDTHISSERVER:             mIsAddThisServer = true; break;
                    case USEGLOBALPREFERREDSERVER:  mIsUseGlobalPreferredServer = true; break;
                }
                break;

            // Email provider, extra info/documentation/instructions
            case IDENTITY: // ignore for now
                break;
            case INPUTFIELD:
                if( mInputFieldInProgress != null )
                    throw new SAXParseException("Nested inputField-tags. This is not allowed!", mLocator);
                mInputFieldInProgress = new InputField();
                mInputFieldInProgress.key = attributes.getValue(ATTRIBUTE.KEY.getXMLStringVersion());
                mInputFieldInProgress.label = attributes.getValue(ATTRIBUTE.LABEL.getXMLStringVersion());
                mAutoconfigInfo.inputFields.add(mInputFieldInProgress);
                mIsInputField = true;
                break;
            case ENABLE:
                if( mInformationBlockInProgress != null )
                    throw new SAXParseException("Illegal nesting of enable-tags, instruction-tags or both.", mLocator);
                mIsEnable = true;
                mInformationBlockInProgress = new InformationBlock();
                mInformationBlockInProgress.url = attributes.getValue(ATTRIBUTE.VISITURL.getXMLStringVersion());
                mAutoconfigInfo.enable = mInformationBlockInProgress;
                break;
            case INSTRUCTION:       // Documentation and stand-alone instruction are exactly the same
            case DOCUMENTATION:     // Nested instruction and description are exactly the same
            case DESCR:
                if( mIsEnable || TAG.toTag(localName) == TAG.DESCR ){    // nested version of instruction in enable tag
                    if( mInformationStringInProgress != null )
                        throw new SAXParseException("Illegal nesting of description or instruction-tags.", mLocator);
                    mInformationStringInProgress = new AutoconfigInfo.ParcelableMutablePair<String, String>
                            (attributes.getValue(ATTRIBUTE.LANG.getXMLStringVersion()),"");
                    mInformationBlockInProgress.descriptions.add(mInformationStringInProgress);
                    mIsInInformationString = true;

                }else{  //  entering standalone version of the instruction tag or documentation tag
                    if( mInputFieldInProgress != null )
                        throw new SAXParseException("Nested instruction-tags. This is not allowed!", mLocator);
                    mInformationBlockInProgress = new InformationBlock();
                    mInformationBlockInProgress.url = attributes.getValue(ATTRIBUTE.URL.getXMLStringVersion());
                    if( TAG.toTag(localName) == TAG.INSTRUCTION )
                        mAutoconfigInfo.instruction = mInformationBlockInProgress;
                    else if ( TAG.toTag(localName) == TAG.DOCUMENTATION )
                        mAutoconfigInfo.documentation = mInformationBlockInProgress;
                    else // this should never happen!
                        throw new SAXParseException("Unknown tag passed through in parser.", mLocator);
                }
                break;

            case CLIENTCONFIGUPDATE:
                mAutoconfigInfo.clientConfigUpdate = attributes.getValue(ATTRIBUTE.URL.getXMLStringVersion());
                break;

            // Shiiiit...
            case NO_VALUE:
            case WRONG_TAG:
            default: throw new SAXParseException("Illegal or unknown tag found.",mLocator);
        }
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {

        String value = new String(ch, start, length);

		// EmailProvider primary extra info
		if          (mIsDomain)             { mAutoconfigInfo.domains.add(value);
                                              mIsDomain = false; }
		else if     (mIsDisplayName)        { mAutoconfigInfo.displayName = value;
                                              mIsDisplayName = false; }
		else if     (mIsDisplayShortName)   { mAutoconfigInfo.displayShortName = value;
                                              mIsDisplayShortName = false; }

		// Incoming Server
		else if     (mIsHostname)           { mServerInProgress.hostname = value;
                                              mIsHostname = false; }
		else if     (mIsPort)               { mServerInProgress.port = Integer.parseInt(value);
                                              mIsPort = false; }
		else if     (mIsSocketType)         { mServerInProgress.socketType = socketTypeMap.get(value);
                                              mIsSocketType = false; }
		else if     (mIsUsername)           { mServerInProgress.username = value;
                                              mIsUsername = false; }
		else if     (mIsAuthentication)     { mServerInProgress.authentication = authenticationTypeMap.get(value);
                                              mIsAuthentication = false; }

		// Pop3
        // These casts are checked to be safe in the startElement method!
		else if     (mIsLeaveMessagesOnServer)      { ((IncomingServerPOP3)mServerInProgress).leaveMessagesOnServer =
                                                            Boolean.parseBoolean(value);
                                                      mIsLeaveMessagesOnServer = false; }
		else if     (mIsDownloadOnBiff)             { ((IncomingServerPOP3)mServerInProgress).downloadOnBiff =
                                                            Boolean.parseBoolean(value);
                                                      mIsDownloadOnBiff = false; }
		else if     (mIsDaysToLeaveMessagesOnServer){ ((IncomingServerPOP3)mServerInProgress).daysToLeaveMessagesOnServer =
                                                            Integer.parseInt(value);
                                                      mIsDaysToLeaveMessagesOnServer = false; }
		// IMAP ? (currently missing)

		// Outgoing Server extras
        // These casts are checked to be safe in the startElement method!
		else if     (mIsAddThisServer)              { ((OutgoingServerSMTP)mServerInProgress).addThisServer =
                                                            Boolean.parseBoolean(value);
                                                      mIsAddThisServer = false; }
		else if     (mIsUseGlobalPreferredServer)   { ((OutgoingServerSMTP)mServerInProgress).useGlobalPreferredServer =
                                                            Boolean.parseBoolean(value);
                                                      mIsUseGlobalPreferredServer = false; }
        // There is only one possibility in the specs for now! This may have to be expanded later ( using the static map )
        else if     (mIsRestriction)                { ((OutgoingServerSMTP)mServerInProgress).restriction =
                                                            RestrictionType.clientIPAddress;
                                                      mIsRestriction = false; }

		// EmailProvider 2nd
        else if     (mIsInputField)                 { mInputFieldInProgress.text = value;
                                                      mIsInputField = false; }
		else if     (mIsInInformationString)        { mInformationStringInProgress.setSecond(value);
                                                      mIsInInformationString = false; }

	}
}
