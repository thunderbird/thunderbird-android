/*
 * AutoconfigInfo.java
 * Author: dzan ( inspired by donated code from Michael Kröz )
 *
 * This class stores all the configuration data we received for a specific emailprovider.
 */

package com.fsck.k9.helper.configxmlparser;

import java.util.ArrayList;
import java.util.List;

import android.util.Pair;

/*
    NOTE: We assume here there is one emailprovider in a file with the data version information in a one-on-one relation

    If there is more the one emailprovider in a file then the parser can construct a list and put identical data version
    information in each of the objects corresponding fields. This would duplicate an integer and a string a few times.

    This assumption was made since it seems to be almost certainly the case at all times. The user only configures one email-
    address at a time, having one domain, having one emailprovider for that domain.
 */


public class AutoconfigInfo {

    /*******************************************************************************
        Define types for some of the data
     *******************************************************************************/
    public static enum AuthenticationType { plain, secure, NTLM, GSSAPI, clientIPaddress, TLSclientcert, none };
    public static enum SocketType { plain, SSL, STARTTLS};
    public static enum RestrictionType { clientIPAddress };

    public static enum ServerType{ IMAP(0), POP3(1), SMTP(2), UNSET(3), NO_VALUE(4), WRONG_TAG(5);

        private int type;
        ServerType(int type){this.type = type;}
        public Server getServerObject(){
            switch(type){
                case 0: return new IncomingServerIMAP();
                case 1: return new IncomingServerPOP3();
                case 2: return new OutgoingServerSMTP();
                default: return null;
            }
        }

        public static ServerType toType(String str){
            try{
                return valueOf(str.toUpperCase());
            }catch (IllegalArgumentException ex){
                return WRONG_TAG;
            }catch (NullPointerException ex){
                return NO_VALUE;
            }
        }
    }


    /*******************************************************************************
        Server types hierarchy
    *******************************************************************************/
    public static abstract class Server{
        public ServerType type;
		public String hostname;
		public int port;
		public SocketType socketType;
		public String username;
		public AuthenticationType authentication;

        public Server(ServerType type){ this.type = type; }
    }

	public static abstract class IncomingServer extends Server{
        public IncomingServer(ServerType type) {super(type);}
    }

    public static abstract class OutgoingServer extends Server{
		public RestrictionType restriction;
        public OutgoingServer(ServerType type) {super(type);}
    }

	public static class IncomingServerPOP3 extends IncomingServer {
        // hardcode the type
        public ServerType type = ServerType.POP3;

        // pop3 options
		public boolean leaveMessagesOnServer;
		public boolean downloadOnBiff;
		public int daysToLeaveMessagesOnServer;
		public int checkInterval;

        // constructor
        public IncomingServerPOP3(){ super(ServerType.POP3); }
	}

	public static class IncomingServerIMAP extends IncomingServer {
        public ServerType type = ServerType.IMAP;

        // constructor
        public IncomingServerIMAP(){ super(ServerType.IMAP); }
	}

	public static class OutgoingServerSMTP extends OutgoingServer {
        // hardcode the type
        public ServerType type = ServerType.SMTP;

        // SMTP options
		public boolean addThisServer;
		public boolean useGlobalPreferredServer;

        // constructor
        public OutgoingServerSMTP(){ super(ServerType.SMTP); }
	}


    /*******************************************************************************
        Other sub classes, wrappers for sets of data in the xml
     *******************************************************************************/
	public static class InputField {
		public String key;
		public String label;
		public String text;
	}

    // Serves for the visit tag and the instruction tag
	public static class InformationBlock {
        public String url;

        // first one is the language, second the text
		public ArrayList<MutablePair<String, String>> descriptions =
                new ArrayList<MutablePair<String, String>>();
	}

    public static class MutablePair<K, V> extends Pair<K, V>{
        private K first;
        private V second;
        public MutablePair(K first, V second) {
            super(first, second);
        }
        public void setFirst(K val){ this.first = val; }
        public void setSecond(V val){ this.second = val; }
    }


    /*******************************************************************************
        Real class specific code
     *******************************************************************************/

    // version info of the data
    public String version;
	public String clientConfigUpdate;

    // General information
    public String id;
	public List<String> domains;
	public String displayName;
	public String displayShortName;

    // Possible servers for this ISP
	public List<Server> incomingServer;
	public List<Server> outgoingServer;

    // Configuration help/information
	public String identity;
	public List<InputField> inputFields;
	public InformationBlock enable;
	public InformationBlock instruction;
    public InformationBlock documentation;


    /*
        Constructor
     */
    public AutoconfigInfo(){
        // initialise the fields
        incomingServer = new ArrayList<Server>();
        outgoingServer = new ArrayList<Server>();
        domains = new ArrayList<String>();
        inputFields = new ArrayList<InputField>();
    }


    /*
        Logic
     */

    // will be added when called for
}
