package ClientProg;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;

public class ServerConnection {
    private InetAddress host;
    private int port;
    private Socket connectionSocket;
    private OutputStream oStr;
    private BufferedReader iStr;
    private String username = null;
    private String password = null;
    private boolean logged = false;

    public ServerConnection(InetAddress host, int port) throws IOException {
        connectionSocket = new Socket(host, port);
        oStr = connectionSocket.getOutputStream();
        iStr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        this.host = host;
        this.port = port;
        System.out.println("Connected to " + host.getHostName() + port );
    }

    public String register(String name, String password, ArrayList<String> tags){
        if(logged){
            return "Already logged in";
        }
        ServerProg.Enrollment stub;
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost",8081);
            stub = (ServerProg.Enrollment) registry.lookup("WINSOME");
            if(stub.register(name, password,tags)){
                return "Iscrizione avvenuta con successo";
            } else {
                return "Nome utente già in uso, provane un altro";
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return "Connessione al server fallita";
        } catch (NullPointerException e){
            return "Riempire tutti i campi: nome password tags";
        }
    }

    public String login(String username, String password) throws IOException {
        if(logged){
            return "Already logged in";
        }
        byte[] message = writeRequest(new String[]{"02", username, password, "\n"});
        oStr.write(message, 0, message.length);
        char[] status = new char[3];
        iStr.read(status, 0, 3);
        int code = Integer.decode(new String(status));
        if(code == 200){
            logged = true;
            this.username = username;
            this.password = password;
            return "logged in";
        } else {
            return new String(status) + ": wrong password or user not registered";
        }
    }

    public String logout(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
        if(username.equals(this.username) && username != null){
            oStr.write("03\n".getBytes(StandardCharsets.UTF_8), 0, "03\n".getBytes(StandardCharsets.UTF_8).length);
            char[] status = new char[3];
            iStr.read(status, 0, 3);
            int code = Integer.decode(new String(status));
            if(code == 200){
                this.logged = false;
                this.username = null;
                this.password = null;
                return "log out completed";
            } else if(code == 401){
                return "unrecognised user";
            } else {
                return new String(status) + "unable to log out";
            }
        } else {
            return "wrong/missing username";
        }
    }

    private byte[] writeRequest(String[] words){
        return String.join(":", words).getBytes(StandardCharsets.UTF_8);
    }
}
