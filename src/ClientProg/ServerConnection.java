package ClientProg;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ServerConnection {
    private final InetAddress host;
    private final int port;
    private Socket connectionSocket;
    private OutputStream oStr;
    private BufferedReader iStr;
    private String username = null;
    private String password = null;
    private boolean logged = false;
    private final FollowerList followers = new FollowerList();

    private Registry registry;
    ServerProg.Enrollment stubSN;
    FollowerCallback callback;

    public ServerConnection(InetAddress host, int port) throws IOException, NotBoundException {
        connectionSocket = new Socket(host, port);
        oStr = connectionSocket.getOutputStream();
        iStr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        this.host = host;
        this.port = port;
        System.out.println("Connected to " + host.getHostName() + port );

        registry = LocateRegistry.getRegistry("localhost",8081);
        stubSN = (ServerProg.Enrollment) registry.lookup("WINSOME");
        callback = (FollowerCallback) UnicastRemoteObject.exportObject(followers, 0);

    }

    public String register(String name, String password, ArrayList<String> tags){
        if(logged){
            return "Already logged in";
        }
        try {
            if(stubSN.register(name, password,tags)){
                return "Iscrizione avvenuta con successo";
            } else {
                return "Nome utente già in uso, provane un altro";
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return "Connessione al server fallita";
        } catch (NullPointerException e){
            return "Riempire tutti i campi: nome password tags";
        }
    }

    public String registerForFollower(){
        if(!logged){
            return "Not logged in";
        }
        try {
            stubSN.registerCallback(callback, username, password);
            return "notifiche attive";
        } catch (RemoteException e) {
            return "notifiche non attive";
        }
    }

    public String unregisterForFollower(){
        if(!logged){
            return "Not logged in";
        }
        try {
            stubSN.unregisterCallback(callback, username, password);
            return "notifiche disattive";
        } catch (RemoteException | NullPointerException e) {
            return "notifiche non disattivate";
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
            return "logged in | " + registerForFollower();
        } else {
            return new String(status) + ": wrong password or user not registered";
        }
    }

    public String logout(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
        if(username.equals(this.username) && username != null){
            String statusRegister = unregisterForFollower();
            oStr.write("03\n".getBytes(StandardCharsets.UTF_8), 0, "03\n".getBytes(StandardCharsets.UTF_8).length);
            char[] status = new char[3];
            iStr.read(status, 0, 3);
            int code = Integer.decode(new String(status));
            if(code == 200){
                this.logged = false;
                this.username = null;
                this.password = null;
                return "log out completed | " + statusRegister;
            } else if(code == 401){
                return "unrecognised user";
            } else {
                return new String(status) + "unable to log out";
            }
        } else {
            return "wrong/missing username";
        }
    }

    public String follow(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"07", username, "\n"});
        oStr.write(message, 0, message.length);
        char[] status = new char[3];
        iStr.read(status, 0, 3);
        int code = Integer.decode(new String(status));
        if(code == 200){
            return "following " + username;
        } else {
            return new String(status) + ": unable to follow";
        }
    }

    public String unfollow(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"08", username, "\n"});
        oStr.write(message, 0, message.length);
        char[] status = new char[3];
        iStr.read(status, 0, 3);
        int code = Integer.decode(new String(status));
        if(code == 200){
            return "unfollowing " + username;
        } else {
            return new String(status) + ": unable to unfollow";
        }
    }
    private byte[] writeRequest(String[] words){
        return String.join(":", words).getBytes(StandardCharsets.UTF_8);
    }
}
