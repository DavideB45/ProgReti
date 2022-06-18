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
    private long lastConnection;

    private int operation;
    private String[] args;
    private final ByteBuffer[] RequestBuffer;
    private ByteBuffer ResponseBuffer;


    public ConnectedUser(SocketChannel uSocket, SelectionKey key) throws IOException {
            sChannel = uSocket;
            lastConnection = System.currentTimeMillis();
            this.key = key;
            sChannel.configureBlocking(false);
            RequestBuffer = new ByteBuffer[]{ByteBuffer.allocate(Integer.BYTES), ByteBuffer.allocate(1024)};
    }

/* old functions
    @Deprecated
    public boolean hasRequest(){
        try {
            if(fileIn.ready()){
                lastConnection = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    @Deprecated
    public int getOpCode() throws IOException {
        String status = fileIn.readLine();
        try{
            return Integer.parseInt(status);
        }catch (NumberFormatException e){
            return -1;
        }
    }
    @Deprecated
    public String[] getArguments() throws IOException {
        String[] args = new String[4];
        int i = -1;
        do{
            i++;
            args[i] = fileIn.readLine();
            System.out.print("<" + args[i] + ">");
        }while (!args[i].equals(""));
        System.out.println();
        return args;
    }
*/

    public void setResponse(int code, String[] values) throws IOException {
        if (ResponseBuffer == null) {
            ResponseBuffer = ByteBuffer.allocate(Integer.BYTES + 1024);
        } else {
            ResponseBuffer.clear();
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
    public boolean sendResponse() throws IOException {
        sChannel.write(ResponseBuffer);
        if (ResponseBuffer.hasRemaining()) {
            return false;
        } else {
            ResponseBuffer.clear();
            return true;
        }
    }
    public boolean readRequest() throws IOException {
        sChannel.read(RequestBuffer);
        int dim = 0;
        if(!RequestBuffer[0].hasRemaining()){
            RequestBuffer[0].flip();
            dim = RequestBuffer[0].getInt();
            System.out.println(dim);
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
    public SelectionKey getKey() {
        return key;
    }
    public int getOperation() {
        return operation;
    }
    public String[] getArgs() throws IOException {
        return args;
    }
    public void disconnect(){
        try {
            sChannel.close();
            key.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setIdentity(Utente user){
        this.identity = user;
    }
    public Utente getIdentity(){
        return this.identity;
    }

    public boolean isConnected(){
        boolean state = sChannel.isOpen() && sChannel.isConnected() && operation != -1;
        return lastConnection + 1000*240 > System.currentTimeMillis();
    }

}
