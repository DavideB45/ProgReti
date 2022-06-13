package ClientProg;

import ServerProg.Post;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

    ObjectMapper mapper = new ObjectMapper();

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
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200){
            logged = true;
            this.username = username;
            this.password = password;
            return "logged in | " + registerForFollower();
        } else {
            return status + ": wrong password or user not registered";
        }
    }
    public String logout(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
        if(username.equals(this.username)){
            String statusRegister = unregisterForFollower();
            oStr.write("03\n\n".getBytes(StandardCharsets.UTF_8), 0, "03\n\n".getBytes(StandardCharsets.UTF_8).length);
            String status = iStr.readLine();
            iStr.readLine();
            int code = Integer.decode(status);
            if(code == 200){
                this.logged = false;
                this.username = null;
                this.password = null;
                return "log out completed | " + statusRegister;
            } else if(code == 401){
                return "unrecognised user";
            } else {
                return status + ": unable to log out";
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
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200){
            return "following " + username;
        } else {
            return status + ": unable to follow";
        }
    }
    public String unfollow(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"08", username, "\n"});
        oStr.write(message, 0, message.length);
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200){
            return "unfollowing " + username;
        } else {
            return status + ": unable to unfollow";
        }
    }
    public String listFollowers() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        ArrayList<SimpleUtente> copy =  followers.getFollowersCopy();
        StringBuilder sb = new StringBuilder();
        for(SimpleUtente u : copy){
            sb.append(u.getUsername() + "\t");
            for (String tag : u.getTags()) {
                sb.append(tag + " ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    public String listFollowing() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"06", "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        if (Integer.decode(status) == 200) {
            ArrayList<SimpleUtente> following = mapper.readValue(iStr.readLine(), new TypeReference<ArrayList<SimpleUtente>>() {});
            StringBuilder sb = new StringBuilder();
            for (SimpleUtente u : following) {
                sb.append(u.getUsername() + "\t");
                for (String tag : u.getTags()) {
                    sb.append(tag + " ");
                }
                sb.append("\n");
            }
            iStr.readLine();
            return sb.toString();
        } else {
            iStr.readLine();
            return status + ": unable to list following";
        }

    }

    public String post(String title, String text) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        SimplePost post = new SimplePost(username, title, text);
        String jsonPost = mapper.writeValueAsString(post);
        byte[] message = writeRequest(new String[]{"10", jsonPost, "\n"});
        oStr.write(message, 0, message.length);
        String status = iStr.readLine();
        int code = Integer.decode(status);
        int postId = Integer.decode(iStr.readLine());
        iStr.readLine();
        if(code == 200){
            return "post published with id " + postId;
        } else {
            return status + ": unable to post";
        }
    }
    public String showPost(String postId) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"12", postId, "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String jsonPost = iStr.readLine();
            iStr.readLine();
            SimplePost post = mapper.readValue(jsonPost, SimplePost.class);
            //return "post by: " + post.getUsername() + " \n " + "title: " + post.getTitle() + " \n " + "text: " + post.getContent();
            return post.toString();
        } else {
            iStr.readLine();
            return status + ": unable to show post";
        }
    }

    private byte[] writeRequest(String[] words){
        return String.join("\n", words).getBytes(StandardCharsets.UTF_8);
    }
}
