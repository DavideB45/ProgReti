package ServerProg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ConnectedUser {
    private Utente identity;
    private final SocketChannel sChannel;
    private final SelectionKey key;

    private int operation;
    private String[] args;
    private final ByteBuffer[] RequestBuffer;
    private ByteBuffer ResponseBuffer;


    public ConnectedUser(SocketChannel uSocket, SelectionKey key) throws IOException {
            sChannel = uSocket;
            this.key = key;
            sChannel.configureBlocking(false);
            RequestBuffer = new ByteBuffer[]{ByteBuffer.allocate(Integer.BYTES), ByteBuffer.allocate(1024)};
    }

    /**
    * set the answer to send
    * first line of answer will contain the code
    * the following one String each
    * terminated by an empty string
    * To send answer see sendResponse
    */
    public void setResponse(int code, String[] values) throws IOException {
        if (ResponseBuffer == null) {
            ResponseBuffer = ByteBuffer.allocate(Integer.BYTES + 2048);
        } else {
            ResponseBuffer.clear();
        }
        int bytes = 10;
        if (values != null){
            for (String value : values) {
                bytes += value.length() + 2;
            }
            if (bytes > ResponseBuffer.capacity() && bytes < 10000) {
                ResponseBuffer = ByteBuffer.allocate(bytes);
            } else if (bytes > 10000) {
                code = 507;
                values = null;
            }
        }
        ResponseBuffer.put(Integer.toString(code).getBytes(StandardCharsets.UTF_8));
        if (values != null){
            for (String arg : values) {
                ResponseBuffer.put("\n".getBytes(StandardCharsets.UTF_8));
                ResponseBuffer.put(arg.getBytes(StandardCharsets.UTF_8));
            }
        }
        ResponseBuffer.put("\n\n".getBytes(StandardCharsets.UTF_8));
        ResponseBuffer.flip();
    }
    /**
    *  called to send a response (non-blocking)
    * return true if answer is fully sent
    * return false otherwise
    */
    public boolean sendResponse() throws IOException {
        sChannel.write(ResponseBuffer);
        if (ResponseBuffer.hasRemaining()) {
            return false;
        } else {
            ResponseBuffer.clear();
            return true;
        }
    }
    /**
    *  called to read a request (non-blocking)
    * return true if answer is fully read or user closed connection
    * false otherwise
    * if completed successfully  getOperation and getArgs will return correct value
    */
    public boolean readRequest() throws IOException {
        if(sChannel.read(RequestBuffer) == -1){
            operation = -1;
            return true;
        }
        int dim = 0;
        if(!RequestBuffer[0].hasRemaining()){
            RequestBuffer[0].flip();
            dim = RequestBuffer[0].getInt();
        } else {
            return false;
        }
        if(RequestBuffer[1].position() == dim){
            RequestBuffer[1].flip();
            String req = new String(RequestBuffer[1].array(), 0, dim, StandardCharsets.UTF_8);
            operation = Integer.parseInt(req.substring(0, req.indexOf("\n")));
            args = req.substring(req.indexOf("\n") + 1).split("\n");
            RequestBuffer[0].clear();
            RequestBuffer[1].clear();
            return true;
        }
        return false;
    }
    /**
     * return the SelectionKey associated to the connection
     */
    public SelectionKey getKey() {
        return key;
    }
    /**
     * return the last operation required by the User
     * if user closed connection return -1
     */
    public int getOperation() {
        return operation;
    }
    /**
     * return the last args read by readRequest
     */
    public String[] getArgs() throws IOException {
        return args;
    }
    /**
     * close the connection
     */
    public void disconnect(){
        try {
            sChannel.close();
            key.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * used to assign to the Object a value representing a registered user
     */
    public void setIdentity(Utente user){
        this.identity = user;
    }
    /**
     * return registered user or null
     */
    public Utente getIdentity(){
        return this.identity;
    }
    /**
    * this value is true if user closed connection
    * before last call to readRequest
    */
    public boolean isConnected(){
        return operation != -1;
    }

}
