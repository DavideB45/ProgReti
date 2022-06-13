package ServerProg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConnectedUser {
    private Utente identity;
    private OutputStream fileOut;
    private BufferedReader fileIn;
    private Socket fileSocket;
    private long lastConnection;


    public ConnectedUser(Socket uSocket) throws IOException {
            fileOut = uSocket.getOutputStream();
            fileIn = new BufferedReader(new InputStreamReader(uSocket.getInputStream()));
            fileSocket = uSocket;
            lastConnection = System.currentTimeMillis();
    }

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

    public int getOpCode() throws IOException {
        String status = fileIn.readLine();
        try{
            return Integer.parseInt(status);
        }catch (NumberFormatException e){
            return -1;
        }
    }

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

    public void answer(String ret) throws IOException {
        fileOut.write(ret.getBytes(StandardCharsets.UTF_8), 0, ret.getBytes(StandardCharsets.UTF_8).length);
    }

    public void setIdentity(Utente user){
        this.identity = user;
    }
    public Utente getIdentity(){
        return this.identity;
    }

    public boolean isConnected(){
        return lastConnection + 1000*240 > System.currentTimeMillis();
    }

}
