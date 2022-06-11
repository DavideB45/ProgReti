package ServerProg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConnectedUser {
    private String name;
    private OutputStream fileOut;
    private BufferedReader fileIn;
    private Socket fileSocket;


    public ConnectedUser(Socket uSocket, String name) throws IOException {
            fileOut = uSocket.getOutputStream();
            fileIn = new BufferedReader(new InputStreamReader(uSocket.getInputStream()));
            fileSocket = uSocket;
            this.name = name;
    }

    public boolean handleRequest(){
        try {
            System.out.print("Req  : ");
            String request = fileIn.readLine();
            System.out.println(request);
            byte[] ciaoRisposta = ("Ciao " + name + "\n").getBytes(StandardCharsets.UTF_8);
            fileOut.write(ciaoRisposta, 0, ciaoRisposta.length);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasRequest(){
        try {
            return this.isConnected() && fileIn.ready();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isConnected(){
        return !fileSocket.isInputShutdown() && !fileSocket.isOutputShutdown();
    }

}
