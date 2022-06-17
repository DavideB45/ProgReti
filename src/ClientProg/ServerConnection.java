package ClientProg;

import ServerProg.Wallet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    Thread multicastThread = null;

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
        int code = Integer.decode(status);
        if(code == 200){
            logged = true;
            this.username = username;
            this.password = password;
            try{
                InetAddress multicast = InetAddress.getByName(iStr.readLine());
                if(!multicast.equals("0")){
                    int port = Integer.decode(iStr.readLine());
                    multicastThread = new Thread(new NotificationReceiver(multicast, port));
                    multicastThread.setDaemon(true);
                    multicastThread.start();
                }
            } catch (IOException e){
                System.out.println("Multicast non attivo");
            }
            iStr.readLine();
            return "logged in | " + registerForFollower();
        } else {
            iStr.readLine();
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
                multicastThread.interrupt();
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
            return post.toString();
        } else {
            iStr.readLine();
            return status + ": unable to show post";
        }
    }
    public String deletePost(String postId) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"13", postId, "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200)
            return "post deleted";
        if(code == 202)
            return "post removed from blog";
        if(code == 401)
            return "unrecognised user";
        if (code == 404)
            return "post not found";
        return status + ": unable to delete post";
    }
    public String rewin(String postId) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"14", postId, "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200)
            return "post rewined";
        if(code == 401)
            return "unrecognised user";
        if (code == 404)
            return "post not found";
        return status + ": unable to rewin post";
    }
    public String rate(String postId, String rating) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"15", postId, rating, "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200)
            return "post rated";
        if(code == 401)
            return "unrecognised user";
        if (code == 404)
            return "post not found";
        if (code == 409)
            return "already rated";
        return status + ": unable to rate post";
    }
    public String comment(String postId, String comment) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"16", postId, comment, "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200)
            return "comment posted";
        if(code == 401)
            return "unrecognised user";
        if (code == 403)
            return "need to be a follower to comment";
        if (code == 404)
            return "post not found";
        if (code == 413)
            return "comment too long";
        return status + ": unable to comment post";
    }

    public String viewBlog() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = "09\n\n".getBytes(StandardCharsets.UTF_8);
        oStr.write(message, 0, message.length);
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String jsonHeads = iStr.readLine();
            iStr.readLine();
            ArrayList<PostHead> heads = mapper.readValue(jsonHeads, new TypeReference<ArrayList<PostHead>>() {});
            StringBuilder sb = new StringBuilder();
            for (int i = heads.size() - 1; i >= 0; i--) {
                PostHead head = heads.get(i);
                sb.append(head.getId() + " " + head.getUsername() + "\n" + head.getTitle() + "\n");
            }
            return sb.toString();
        } else {
            iStr.readLine();
            return status + ": unable to view blog";
        }
    }
    public String showFeed() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"11", "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String jsonPosts = iStr.readLine();
            iStr.readLine();
            ArrayList<PostHead> feed = mapper.readValue(jsonPosts, new TypeReference<ArrayList<PostHead>>() {});
            StringBuilder sb = new StringBuilder();
            for (PostHead post : feed) {
                sb.append(post.getId() + " " + post.getUsername() + "\n" + post.getTitle() + "\n");
            }
            return sb.toString();
        } else {
            iStr.readLine();
            return status + ": unable to show feed";
        }
    }

    public String wallet() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"17", "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String jsonWallet = iStr.readLine();
            iStr.readLine();
            SimpleWallet wallet = mapper.readValue(jsonWallet, SimpleWallet.class);
            return wallet.toString();
        } else {
            iStr.readLine();
            return status + ": unable to show wallet";
        }
    }
    public String getWalletInBitcoin() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        byte[] message = writeRequest(new String[]{"18", "\n"});
        oStr.write(message);
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String btc = iStr.readLine();
            iStr.readLine();
            return btc + " BTC";
        } else if (code == 503){
            iStr.readLine();
            return "service unavailable";
        } else {
            return code + ": unable to process";
        }
    }

    private byte[] writeRequest(String[] words){
        return String.join("\n", words).getBytes(StandardCharsets.UTF_8);
    }
}
