package ClientProg;

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

    ServerProg.Enrollment stubSN;
    FollowerCallback callback;

    ObjectMapper mapper = new ObjectMapper();

    public ServerConnection(InetAddress host, int portTCP) throws IOException, NotBoundException {
        this.host = host;
        this.port = portTCP;
        connectionSocket = new Socket(host, port);
        oStr = connectionSocket.getOutputStream();
        iStr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        System.out.println("Connected to " + host.getHostName() + port );

        writeRequest(new String[]{"00", "\n"});
        iStr.readLine();
        String hostname = iStr.readLine();
        int rmiPort = Integer.parseInt(iStr.readLine());
        iStr.readLine();
        if(rmiPort != -1) {
            Registry registry = LocateRegistry.getRegistry(hostname, rmiPort);
            stubSN = (ServerProg.Enrollment) registry.lookup("WINSOME");
            callback = (FollowerCallback) UnicastRemoteObject.exportObject(followers, 0);
        }
    }
    public boolean reconnect(){
        try {
            if(oStr != null && iStr != null) {
                oStr.close();
                iStr.close();
                connectionSocket.close();
            }
            logged = false;
            connectionSocket = new Socket(host, port);
            oStr = connectionSocket.getOutputStream();
            iStr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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
            UnicastRemoteObject.unexportObject(followers, false);
            return "notifiche disattive";
        } catch (RemoteException | NullPointerException e) {
            return "notifiche non disattivate";
        }
    }

    public String login(String username, String password) throws IOException {
        if(logged){
            return "Already logged in";
        }
        writeRequest(new String[]{"02", username, password, "\n"});
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200){
            logged = true;
            this.username = username;
            this.password = password;
            try{
                InetAddress multicast = InetAddress.getByName(iStr.readLine());
                int port = Integer.decode(iStr.readLine());
                multicastThread = new Thread(new NotificationReceiver(multicast, port));
                multicastThread.setDaemon(true);
                multicastThread.start();
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
    public String logout() throws IOException {
        if(!logged){
            return "Not logged in";
        }
        String statusRegister = unregisterForFollower();
        writeRequest(new String[]{"03","\n"});
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
    }

    public String follow(String username) throws IOException {
        if(!logged){
            return "Not logged in";
        }
       writeRequest(new String[]{"07", username, "\n"});
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
        writeRequest(new String[]{"08", username, "\n"});
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
        writeRequest(new String[]{"06", "\n"});
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
    public String listUsers() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        writeRequest(new String[]{"04", "\n"});
        String status = iStr.readLine();
        if (Integer.decode(status) == 200) {
            ArrayList<SimpleUtente> users = mapper.readValue(iStr.readLine(), new TypeReference<ArrayList<SimpleUtente>>() {});
            StringBuilder sb = new StringBuilder();
            for (SimpleUtente u : users) {
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
            return status + ": unable to list users";
        }
    }

    public String post(String title, String text) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        SimplePost post = new SimplePost(username, title, text);
        String jsonPost = mapper.writeValueAsString(post);
        writeRequest(new String[]{"10", jsonPost, "\n"});
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if(code == 200){
            int postId = Integer.decode(iStr.readLine());
            iStr.readLine();
            return "post published with id " + postId;
        } else {
            iStr.readLine();
            return status + ": unable to post";
        }
    }
    public String showPost(String postId) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        writeRequest(new String[]{"12", postId, "\n"});
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
        writeRequest(new String[]{"13", postId, "\n"});
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
        writeRequest(new String[]{"14", postId, "\n"});
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
        writeRequest(new String[]{"15", postId, rating, "\n"});
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
        writeRequest(new String[]{"16", postId, comment, "\n"});
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
        writeRequest(new String[]{"09", "\n"});
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
        writeRequest(new String[]{"11", "\n"});
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
        writeRequest(new String[]{"17", "\n"});
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
        writeRequest(new String[]{"18", "\n"});
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

    private void writeRequest(String[] words) throws IOException {
        byte[] message = String.join("\n", words).getBytes(StandardCharsets.UTF_8);
        int mLen = message.length;
        byte[] length = new byte[Integer.BYTES];
        for (int i = 0; i < length.length; i++) {
            length[length.length - i - 1] = (byte) (mLen & 0xFF);
            mLen >>= 8;
        }
        oStr.write(length);
        oStr.write(message);
    }
}
