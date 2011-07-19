/*
 * AutoconfigInfo.java
 * Author: dzan ( inspired by donated code from Michael Kröz )
 *
 * This class stores all the configuration data we received for a specific emailprovider.
 */

package com.fsck.k9.helper.configxmlparser;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

/*
    NOTE: We assume here there is one emailprovider in a file with the data version information in a one-on-one relation

    If there is more the one emailprovider in a file then the parser can construct a list and put identical data version
    information in each of the objects corresponding fields. This would duplicate an integer and a string a few times.

    This assumption was made since it seems to be almost certainly the case at all times. The user only configures one email-
    address at a time, having one domain, having one emailprovider for that domain.
 */


public class AutoconfigInfo implements Parcelable {

    /*******************************************************************************
        Fullfil the parcelable interface ( creator part )
     *******************************************************************************/

    public static final Parcelable.Creator<AutoconfigInfo> CREATOR
        = new Parcelable.Creator<AutoconfigInfo>() {

        // using a custom constructor to implement this
        @Override
        public AutoconfigInfo createFromParcel(Parcel parcel) {
            return new AutoconfigInfo(parcel);
        }

        @Override
        public AutoconfigInfo[] newArray(int i) {
            return new AutoconfigInfo[i];
        }
    };

    /*******************************************************************************
        Define types for some of the data
     *******************************************************************************/
    public static enum AuthenticationType { plain, secure, NTLM, GSSAPI, clientIPaddress, TLSclientcert, none, UNSET };
    public static enum SocketType { plain, SSL, STARTTLS, UNSET};
    public static enum RestrictionType { clientIPAddress };

    public static enum ServerType{ IMAP(0), POP3(1), SMTP(2), UNSET(3), NO_VALUE(4), WRONG_TAG(5);

        private int type;
        ServerType(int type){this.type = type;}
        public Server getServerObject(Parcel parcel){
            // ugly here but cleanest solution to mix the parcelable and inheritance in the server classes ( user of super() )
            switch(type){
                case 0: if( parcel != null ) return new IncomingServerIMAP(parcel);
                        else return new IncomingServerIMAP();
                case 1: if( parcel != null )return new IncomingServerPOP3(parcel);
                        else return new IncomingServerPOP3();
                case 2: if( parcel != null ) return new OutgoingServerSMTP(parcel);
                        else return new OutgoingServerSMTP();
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
    public static abstract class Server implements Parcelable{
        public ServerType type;
		public String hostname;
		public int port;
		public SocketType socketType;
		public String username;
		public AuthenticationType authentication;

        public Server(ServerType type){
            if( type != null )
                this.type = type;
            else this.type = ServerType.UNSET;
        }

        public Server(Parcel parcel, ServerType type){
            this(type);
            hostname = parcel.readString();
            port = parcel.readInt();
            socketType = SocketType.valueOf(parcel.readString());
            username = parcel.readString();
            authentication = AuthenticationType.valueOf(parcel.readString());
        }

        public static final Parcelable.Creator<Server> CREATOR
            = new Parcelable.Creator<Server>() {
            @Override
            public Server createFromParcel(Parcel parcel) {
                return ServerType.toType(parcel.readString()).getServerObject(parcel);
            }

            @Override
            public Server[] newArray(int i) { return new Server[i]; }
        };

        @Override
        public int describeContents() { return this.hashCode(); }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            if( socketType == null ) socketType = SocketType.UNSET;
            if( type == null ) type = ServerType.UNSET;
            if( authentication == null ) authentication = AuthenticationType.UNSET;

            parcel.writeString(hostname);
            parcel.writeInt(port);
            parcel.writeString(socketType.name());
            parcel.writeString(username);
            parcel.writeString(authentication.name());
        }
    }

	public static abstract class IncomingServer extends Server{
        public IncomingServer(ServerType type) {super(type);}
        public IncomingServer(Parcel parcel, ServerType type) {super(parcel, type);}
    }

    public static abstract class OutgoingServer extends Server{
		public RestrictionType restriction;
        public OutgoingServer(ServerType type) {super(type);}
        public OutgoingServer(Parcel parcel, ServerType type) {super(parcel, type);}
    }

	public static class IncomingServerPOP3 extends IncomingServer {
        // hardcode the type
        public ServerType type = ServerType.POP3;

        // pop3 options
		public boolean leaveMessagesOnServer;
		public boolean downloadOnBiff;
		public int daysToLeaveMessagesOnServer;
		public int checkInterval;

        // constructors
        public IncomingServerPOP3(){ super(ServerType.POP3); }

        public IncomingServerPOP3(Parcel parcel){
            super(parcel, ServerType.POP3);

            // load in extras
            leaveMessagesOnServer = ( parcel.readInt() == 1 ) ? true : false;
            downloadOnBiff = ( parcel.readInt() == 1 ) ? true : false;
            daysToLeaveMessagesOnServer = parcel.readInt();
            checkInterval = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);

            // extra fields in this class
            parcel.writeInt(leaveMessagesOnServer ? 1 : 0);
            parcel.writeInt(downloadOnBiff ? 1 : 0);
            parcel.writeInt(daysToLeaveMessagesOnServer);
            parcel.writeInt(checkInterval);
        }
    }

	public static class IncomingServerIMAP extends IncomingServer {
        public ServerType type = ServerType.IMAP;

        // constructor
        public IncomingServerIMAP(){ super(ServerType.IMAP); }
        public IncomingServerIMAP(Parcel parcel){
            super(parcel, ServerType.IMAP);
        }
	}

	public static class OutgoingServerSMTP extends OutgoingServer {
        // hardcode the type
        public ServerType type = ServerType.SMTP;

        // SMTP options
		public boolean addThisServer;
		public boolean useGlobalPreferredServer;

        // constructor
        public OutgoingServerSMTP(){ super(ServerType.SMTP); }
        public OutgoingServerSMTP(Parcel parcel){
            super(parcel, ServerType.SMTP);

            // load in extras
            addThisServer = ( parcel.readInt() == 1 ) ? true : false;
            useGlobalPreferredServer = ( parcel.readInt() == 1 ) ? true : false;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);

            // extra fields in this class
            parcel.writeInt(addThisServer ? 1 : 0);
            parcel.writeInt(useGlobalPreferredServer ? 1 : 0);
        }
	}


    /*******************************************************************************
        Other sub classes, wrappers for sets of data in the xml
     *******************************************************************************/
	public static class InputField implements Parcelable{

        public static final Parcelable.Creator<InputField> CREATOR
            = new Parcelable.Creator<InputField>() {
            @Override
            public InputField createFromParcel(Parcel parcel) { return new InputField(parcel); }
            @Override
            public InputField[] newArray(int i) { return new InputField[i]; }
        };

		public String key;
		public String label;
		public String text;

        public InputField(Parcel parcel){
            key = parcel.readString();
            label = parcel.readString();
            text = parcel.readString();
        }

        // keep default constructor
        public InputField() {}

        @Override
        public int describeContents() { return hashCode(); }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(key);
            parcel.writeString(label);
            parcel.writeString(text);
        }
    }

    // Serves for the visit tag and the instruction tag
	public static class InformationBlock implements Parcelable{

        public static final Parcelable.Creator<InformationBlock> CREATOR
            = new Parcelable.Creator<InformationBlock>() {
            @Override
            public InformationBlock createFromParcel(Parcel parcel) { return new InformationBlock(parcel); }
            @Override
            public InformationBlock[] newArray(int i) { return new InformationBlock[i]; }
        };

        // fields
        public String url;
        // first one is the language, second the text
		public ArrayList<ParcelableMutablePair<String, String>> descriptions = new ArrayList<ParcelableMutablePair<String, String>>();

        public InformationBlock(Parcel parcel){
            url = parcel.readString();
            descriptions = parcel.readArrayList(ParcelableMutablePair.class.getClassLoader());
        }

        // keep the default constructor too
        public InformationBlock(){}

        @Override
        public int describeContents() { return hashCode(); }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(url);
            parcel.writeList(descriptions);
        }
    }

    // This class is pretty 'unstable'. We could force the generic classes to be classes implementing the parcelable
    // interface but since primitive types and others who are parcelable do not extends this interface we would have to create
    // a lot of specific classes accepting for example Strings, int's,.... So we just do it this way and trust the programmer using
    // it to be sure it is only used with parcelable stuff.
    public static class ParcelableMutablePair<K, V> extends Pair<K, V> implements Parcelable{

        public static final Parcelable.Creator<ParcelableMutablePair> CREATOR
            = new Parcelable.Creator<ParcelableMutablePair>() {
            @Override
            public ParcelableMutablePair createFromParcel(Parcel parcel) { return new ParcelableMutablePair(parcel); }
            @Override
            public ParcelableMutablePair[] newArray(int i) { return new ParcelableMutablePair[i]; }
        };

        private K first;
        private V second;

        public ParcelableMutablePair(K first, V second) {
            super(first, second);
        }

        public ParcelableMutablePair(Parcel parcel){
           super(null, null);
           first = (K) parcel.readValue(first.getClass().getClassLoader());
           second = (V) parcel.readValue(second.getClass().getClassLoader());
        }

        public void setFirst(K val){ this.first = val; }
        public void setSecond(V val){ this.second = val; }

        @Override
        public int describeContents() {return hashCode();}

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            // NOTE: read info above class declaration!!!!
            parcel.writeValue(first);
            parcel.writeValue(second);
        }
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
        Constructors
     */
    public AutoconfigInfo(){
        // initialise the fields
        incomingServer = new ArrayList<Server>();
        outgoingServer = new ArrayList<Server>();
        domains = new ArrayList<String>();
        inputFields = new ArrayList<InputField>();
    }

    public AutoconfigInfo(Parcel parcel){
        version = parcel.readString();
        clientConfigUpdate = parcel.readString();

        id = parcel.readString();
        domains = parcel.readArrayList(String.class.getClassLoader());
        displayName = parcel.readString();
        displayShortName = parcel.readString();


    }

    /*
        Logic
     */

    // returns a list of available incoming server types
    public List<ServerType> getAvailableIncomingServerTypes(){
        ArrayList<ServerType> types = new ArrayList<ServerType>();
        // for now there is only support for imap & pop3 so this is a bit overkill
        for( Server serv : incomingServer )
            if( !types.contains(serv.type) ) types.add(serv.type);
        return types;
    }

    // returns a list of available outgoing server types
    public List<ServerType> getAvailableOutgoingServerTypes(){
        ArrayList<ServerType> types = new ArrayList<ServerType>();
        // THERE IS ONLY SMTP FOR NOW
        /*for( Server serv : outgoingServer )
            if( !types.contains(serv.type) ) types.add(serv.type);*/
        if( outgoingServer.size() > 0 ) types.add(ServerType.SMTP);
        return types;
    }

    // filters the list of servers according to the arguments ( list of allowed specifications )
    private List<Server> getFilteredServerList
            (List<Server> serverList, List<ServerType> serverTypes, List<AuthenticationType> authenticationTypes, List<SocketType> socketTypes){
        ArrayList<Server> servers = new ArrayList<Server>();
        ArrayList<Server> toBeRemoved = new ArrayList<Server>();

        // filter for servertypes
        if( serverTypes != null && !serverTypes.isEmpty() )
            for( Server serv : serverList )
                if( serverTypes.contains(serv.type))
                    servers.add(serv);

        // filter for autenticationtype
        if( authenticationTypes != null & !authenticationTypes.isEmpty() ){
            for( Server serv : servers )
                if( !authenticationTypes.contains(serv.authentication) )
                    toBeRemoved.add(serv);
        }
        servers.removeAll(toBeRemoved);
        toBeRemoved.clear();

        // filter for sockettype
        if( socketTypes != null & !socketTypes.isEmpty() )
            for( Server serv : servers )
                if( !socketTypes.contains(serv.socketType) )
                    toBeRemoved.add(serv);
        servers.removeAll(toBeRemoved);

        return servers;
    }

    // public wrappers for the filter method
    public List<Server> getOutgoingServers
            (List<ServerType> serverTypes, List<AuthenticationType> authenticationTypes, List<SocketType> socketTypes){
        return getFilteredServerList(outgoingServer, serverTypes, authenticationTypes, socketTypes);
    }

    public List<Server> getIncomingServers
            (List<ServerType> serverTypes, List<AuthenticationType> authenticationTypes, List<SocketType> socketTypes){
        return getFilteredServerList(incomingServer, serverTypes, authenticationTypes, socketTypes);
    }

    /*
        What's left of the parcelable interface
     */
    @Override
    public int describeContents(){ return hashCode(); }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(version);
        parcel.writeString(clientConfigUpdate);

        parcel.writeString(id);
        parcel.writeList(domains);
        parcel.writeString(displayName);
        parcel.writeString(displayShortName);

        parcel.writeList(incomingServer);
        parcel.writeList(outgoingServer);

        parcel.writeString(identity);
        parcel.writeList(inputFields);
        parcel.writeValue(enable);
        parcel.writeValue(instruction);
        parcel.writeValue(documentation);
    }
}

