package org.eclipse.paho.android.sample.model;


import org.eclipse.paho.android.sample.activity.Connection;

public class ConnectionModel {

    private static final String CLIENT_HANDLE = "CLIENT_HANDLE";
    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String HOST_NAME = "HOST_NAME";
    private static final String PORT = "PORT";
    private static final String CLEAN_SESSION = "CLEAN_SESSION";
    private static final String USERNAME = "USERNAME";
    private static final String PASSWORD = "PASSWORD";
    private static final String TLS_SERVER_KEY = "TLS_SERVER_KEY";
    private static final String TLS_CLIENT_KEY = "TLS_CLIENT_KEY";
    private static final String TIMEOUT = "TIMEOUT";
    private static final String KEEP_ALIVE = "KEEP_ALIVE";
    private static final String LWT_TOPIC = "LWT_TOPIC";
    private static final String LWT_MESSAGE = "LWT_MESSAGE";
    private static final String LWT_QOS = "LWT_QOS";
    private static final String LWT_RETAIN = "LWT_RETAIN";


    private String clientHandle = "";
    private String clientId = "AndroidExampleClient";
    private String serverHostName = "iot.eclipse.org";
    private int serverPort = 1883;
    private boolean cleanSession = true;
    private String username = "";
    private String password = "";


    private boolean tlsConnection = false;
    private String tlsServerKey = "";
    private String tlsClientKey = "";
    private int timeout = 80;
    private int keepAlive = 200;
    private String lwtTopic = "";
    private String lwtMessage = "";
    private int lwtQos = 0;
    private boolean lwtRetain =  false;

    public ConnectionModel(){

    }



    /** Initialise the ConnectionModel with an existing connection **/
    public ConnectionModel(Connection connection){
        clientHandle = connection.handle();
        clientId = connection.getId();
        serverHostName = connection.getHostName();
        serverPort = connection.getPort();
        cleanSession = connection.getConnectionOptions().isCleanSession();

        if(connection.getConnectionOptions().getUserName() == null){
            username = "";
        }else {
            username = connection.getConnectionOptions().getUserName();
        }
        if(connection.getConnectionOptions().getPassword() != null) {
            password = new String(connection.getConnectionOptions().getPassword());
        } else {
            password = "";
        }
        tlsServerKey = "--- TODO ---";
        tlsClientKey = "--- TODO ---";
        timeout = connection.getConnectionOptions().getConnectionTimeout();
        keepAlive = connection.getConnectionOptions().getKeepAliveInterval();

        if(connection.getConnectionOptions().getWillDestination() == null){
            lwtTopic = "";
        } else {
            lwtTopic = connection.getConnectionOptions().getWillDestination();
        }
        if(connection.getConnectionOptions().getWillMessage() != null) {
            lwtMessage = new String(connection.getConnectionOptions().getWillMessage().getPayload());
            lwtQos = connection.getConnectionOptions().getWillMessage().getQos();
            lwtRetain = connection.getConnectionOptions().getWillMessage().isRetained();
        } else {
            lwtMessage = "";
            lwtQos = 0;
            lwtRetain = false;
        }

    }

    public String getClientHandle() {
        return clientHandle;
    }

    public void setClientHandle(String clientHandle) {
        this.clientHandle = clientHandle;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getServerHostName() {
        return serverHostName;
    }

    public void setServerHostName(String serverHostName) {
        this.serverHostName = serverHostName;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTlsServerKey() {
        return tlsServerKey;
    }

    public void setTlsServerKey(String tlsServerKey) {
        this.tlsServerKey = tlsServerKey;
    }

    public String getTlsClientKey() {
        return tlsClientKey;
    }

    public void setTlsClientKey(String tlsClientKey) {
        this.tlsClientKey = tlsClientKey;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getLwtTopic() {
        return lwtTopic;
    }

    public void setLwtTopic(String lwtTopic) {
        this.lwtTopic = lwtTopic;
    }

    public String getLwtMessage() {
        return lwtMessage;
    }

    public void setLwtMessage(String lwtMessage) {
        this.lwtMessage = lwtMessage;
    }

    public int getLwtQos() {
        return lwtQos;
    }

    public void setLwtQos(int lwtQos) {
        this.lwtQos = lwtQos;
    }

    public boolean isLwtRetain() {
        return lwtRetain;
    }

    public void setLwtRetain(boolean lwtRetain) {
        this.lwtRetain = lwtRetain;
    }

    public boolean isTlsConnection() {
        return tlsConnection;
    }

    public void setTlsConnection(boolean tlsConnection) {
        this.tlsConnection = tlsConnection;
    }

    @Override
    public String toString() {
        return "ConnectionModel{" +
                "clientHandle='" + clientHandle + '\'' +
                ", clientId='" + clientId + '\'' +
                ", serverHostName='" + serverHostName + '\'' +
                ", serverPort=" + serverPort +
                ", cleanSession=" + cleanSession +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", tlsConnection=" + tlsConnection +
                ", tlsServerKey='" + tlsServerKey + '\'' +
                ", tlsClientKey='" + tlsClientKey + '\'' +
                ", timeout=" + timeout +
                ", keepAlive=" + keepAlive +
                ", lwtTopic='" + lwtTopic + '\'' +
                ", lwtMessage='" + lwtMessage + '\'' +
                ", lwtQos=" + lwtQos +
                ", lwtRetain=" + lwtRetain +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectionModel that = (ConnectionModel) o;

        if (serverPort != that.serverPort) return false;
        if (cleanSession != that.cleanSession) return false;
        if (tlsConnection != that.tlsConnection) return false;
        if (timeout != that.timeout) return false;
        if (keepAlive != that.keepAlive) return false;
        if (lwtQos != that.lwtQos) return false;
        if (lwtRetain != that.lwtRetain) return false;
        if (clientHandle != null ? !clientHandle.equals(that.clientHandle) : that.clientHandle != null)
            return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null)
            return false;
        if (serverHostName != null ? !serverHostName.equals(that.serverHostName) : that.serverHostName != null)
            return false;
        if (username != null ? !username.equals(that.username) : that.username != null)
            return false;
        if (password != null ? !password.equals(that.password) : that.password != null)
            return false;
        if (tlsServerKey != null ? !tlsServerKey.equals(that.tlsServerKey) : that.tlsServerKey != null)
            return false;
        if (tlsClientKey != null ? !tlsClientKey.equals(that.tlsClientKey) : that.tlsClientKey != null)
            return false;
        if (lwtTopic != null ? !lwtTopic.equals(that.lwtTopic) : that.lwtTopic != null)
            return false;
        return !(lwtMessage != null ? !lwtMessage.equals(that.lwtMessage) : that.lwtMessage != null);

    }

    @Override
    public int hashCode() {
        int result = clientHandle != null ? clientHandle.hashCode() : 0;
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (serverHostName != null ? serverHostName.hashCode() : 0);
        result = 31 * result + serverPort;
        result = 31 * result + (cleanSession ? 1 : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (tlsConnection ? 1 : 0);
        result = 31 * result + (tlsServerKey != null ? tlsServerKey.hashCode() : 0);
        result = 31 * result + (tlsClientKey != null ? tlsClientKey.hashCode() : 0);
        result = 31 * result + timeout;
        result = 31 * result + keepAlive;
        result = 31 * result + (lwtTopic != null ? lwtTopic.hashCode() : 0);
        result = 31 * result + (lwtMessage != null ? lwtMessage.hashCode() : 0);
        result = 31 * result + lwtQos;
        result = 31 * result + (lwtRetain ? 1 : 0);
        return result;
    }
}
