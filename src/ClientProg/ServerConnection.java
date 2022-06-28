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
    boolean localFollowers;

    Thread multicastThread = null;

    ServerProg.Enrollment stubSN;
    FollowerCallback callback;

    ObjectMapper mapper = new ObjectMapper();

    public ServerConnection(String configFile) throws IOException, NotBoundException {
        File file = new File(configFile);
        this.host = InetAddress.getByName(getFromConfig(file, "SERVER_IP", "localhost"));
        this.port = Integer.decode(getFromConfig(file, "SERVER_PORT", "8080"));
        connectionSocket = new Socket(host, port);
        oStr = connectionSocket.getOutputStream();
        iStr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        System.out.println("Connected to " + host.getHostName() + port );
        this.localFollowers = Boolean.parseBoolean(getFromConfig(file, "FOLLOWER_LOCAL", "false"));
        writeRequest(new String[]{"00", "\n"});
        iStr.readLine();
        String hostname = iStr.readLine();
        int rmiPort = Integer.parseInt(iStr.readLine());
        iStr.readLine();
        if(rmiPort != -1) {
            Registry registry = LocateRegistry.getRegistry(hostname, rmiPort);
            stubSN = (ServerProg.Enrollment) registry.lookup("WINSOME");
        }
    }

    /**
     * try to reconnect to the server
     * @return true if connection reestablished
     */
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

    /**
     * clore the connection with the server
     */
    public void closeConnection() throws IOException {
        try {
            logout();
        } catch (IOException ignore) {
        }
        oStr.close();
        iStr.close();
        connectionSocket.close();
    }


    /**
     * register a new user to the social network
     * @param name the username
     * @param password a password
     * @param tags things user likes
     * @return a String explaining the status of the request
     */
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

    /**
     * send a request to register for RMI callback
     * @return a String explaining the status of the request
     */
    public String registerForFollower(){
        if(!logged){
            return "Not logged in";
        }
        if(!localFollowers){
            return "follower locali dsattivati";
        }
        try {
            callback = (FollowerCallback) UnicastRemoteObject.exportObject(followers, 0);
            stubSN.registerCallback(callback, username, password);
            return "notifiche attive";
        } catch (RemoteException e) {
            return "notifiche non attive";
        }
    }

    /**
     * send a request to unregister for RMI callback
     * @return a String explaining the status of the request
     */
    public String unregisterForFollower(){
        if(!logged){
            return "Not logged in";
        }
        if(!localFollowers){
            return "follower locali dsattivati";
        }
        try {
            stubSN.unregisterCallback(callback, username, password);
            UnicastRemoteObject.unexportObject(followers, false);
            return "notifiche disattive";
        } catch (RemoteException | NullPointerException e) {
            return "notifiche non disattivate";
        }
    }

    /**
     * tru to login using username and password
     * @param username the username
     * @param password the password
     * @return a String explaining the status of the request
     * @throws IOException if a problem with connection occurs
     */
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

    /**
     * logout from the server
     * @return a String explaining the status of the request
     * @throws IOException if a problem with connection occurs
     */
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
            if(multicastThread != null)
                multicastThread.interrupt();
            return "log out completed | " + statusRegister;
        } else if(code == 401){
            return "unrecognised user";
        } else {
            return status + ": unable to log out";
        }
    }

    /**
     * start to follow user
     * @param username the user to follow
     * @return a String explaining the status of the request
     * @throws IOException if a problem with connection occurs
     */
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

    /**
     * stop following user
     * @param username the user to unfollow
     * @return a String explaining the status of the request
     * @throws IOException if a problem with connection occurs
     */
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

    /**
     * @return a String showing followers or an error
     * @throws IOException if a problem with connection occurs
     */
    public String listFollowers() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        if(localFollowers) {
            ArrayList<SimpleUtente> copy = followers.getFollowersCopy();
            return "FOLLOWERS      || TAGS\n" +
                    prettifySimpleUser(copy);
        } else {
            writeRequest(new String[]{"05", "\n"});
            String status = iStr.readLine();
            if (Integer.decode(status) == 200) {
                ArrayList<SimpleUtente> copy = mapper.readValue(iStr.readLine(), new TypeReference<ArrayList<SimpleUtente>>() {});
                iStr.readLine();
                return "FOLLOWERS      || TAGS\n" +
                        prettifySimpleUser(copy);
            } else {
                iStr.readLine();
                return status + ": unable to list followers";
            }
        }
    }

    /**
     * @return a string showing following or an error
     */
    public String listFollowing() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        writeRequest(new String[]{"06", "\n"});
        String status = iStr.readLine();
        if (Integer.decode(status) == 200) {
            ArrayList<SimpleUtente> following = mapper.readValue(iStr.readLine(), new TypeReference<ArrayList<SimpleUtente>>() {});
            iStr.readLine();
            return "FOLLOWING      || TAGS\n" +
                    prettifySimpleUser(following);
        } else {
            iStr.readLine();
            return status + ": unable to list following";
        }

    }

    /**
     * @return a String showing users with at least a tag in common oe an error
     */
    public String listUsers() throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        writeRequest(new String[]{"04", "\n"});
        String status = iStr.readLine();
        if (Integer.decode(status) == 200) {
            ArrayList<SimpleUtente> users = mapper.readValue(iStr.readLine(), new TypeReference<ArrayList<SimpleUtente>>() {});
            iStr.readLine();
            return "USERS          || TAGS\n" +
                    prettifySimpleUser(users);
        } else {
            iStr.readLine();
            return status + ": unable to list users";
        }
    }

    /**
     * create a post
     * @param title the title of the post
     * @param text the content of the post
     * @return the id of the new post or an error
     */
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

    /**
     * @param postId the post to show
     * @return a string representing the post or an error
     */
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

    /**
     * @param postId the post to get
     * @return the post or null
     * @throws IOException if a problem with connection occurs
     */
    public SimplePost showPostObj(String postId) throws IOException {
        if (!logged) {
            return null;
        }
        writeRequest(new String[]{"12", postId, "\n"});
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String jsonPost = iStr.readLine();
            iStr.readLine();
            return mapper.readValue(jsonPost, SimplePost.class);
        } else {
            iStr.readLine();
            return null;
        }
    }
    /**
     * remove post from user's blog
     * and if connected user is the creator remove it from the social network
     * @param postId the post to delete
     * @return a String explaining the result
     */
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
        return status + ": unable to delete";
    }

    /**
     * add a post to user's blog if not already in his blog
     * @param postId the post to rewin
     * @return a String explaining the status
     */
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

    /**
     * rate a post
     * @param postId post to rate
     * @param rating number representing vote
     * @return String explaining the status
     */
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

    /**
     * add comment to a post
     * @param postId post to comment
     * @param comment comment to leave
     * @return String explaining the result
     */
    public String comment(String postId, String comment) throws IOException {
        if (!logged) {
            return "Not logged in";
        }
        if(comment == null || comment.equals(""))
            return "comment cannot be empty";
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

    /**
     * @return a string representing posts in user's blog or an error
     */
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
                sb.append("------------------------------\n");
                sb.append(prettifyPost(head));
            }
            sb.append("------------------------------\n");
            return sb.toString();
        } else {
            iStr.readLine();
            return status + ": unable to view blog";
        }
    }

    /**
     * @return list of user's posts or null
     */
    public ArrayList<PostHead> viewBlogObj() throws IOException {
        if (!logged) {
            return null;
        }
        writeRequest(new String[]{"09", "\n"});
        String status = iStr.readLine();
        int code = Integer.decode(status);
        if (code == 200) {
            String jsonHeads = iStr.readLine();
            iStr.readLine();
            return mapper.readValue(jsonHeads, new TypeReference<ArrayList<PostHead>>() {});
        } else {
            iStr.readLine();
            return null;
        }
    }
    /**
     * @return a string representing posts in user's feed
     */
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
                sb.append("------------------------------\n");
                sb.append(prettifyPost(post));
            }
            sb.append("------------------------------\n");
            return sb.toString();
        } else {
            iStr.readLine();
            return status + ": unable to show feed";
        }
    }

    /**
     * @return a string representing user's revenue or an error
     */
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

    /**
     * @return the value of wallet in BTC or an error
     */
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

    /**
     * send a request to the server
     * @param words array of parameters (starting with operation number)
     * @throws IOException if unable to send request
     */
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

    /**
     * read a value from a configuration file
     * @param config file to read from
     * @param key name of the value
     * @param defaultValue value used if a problem occurs
     * @return the value of key
     */
    private String getFromConfig(File config, String key, String defaultValue){
        String value = "";
        if (config.canRead()){
            try (BufferedReader br = new BufferedReader(new FileReader(config))){
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.replaceAll(" ","").split("=");
                    if (parts[0].equals(key)) {
                        value = parts[1];
                        break;
                    }
                }
            } catch (IOException ignored){}
        }
        if (value.isEmpty()) {
            System.out.println(key + " " + defaultValue);
            return defaultValue;
        }
        System.out.println(key + " " + value);
        return value;
    }

    /**
     * @param users a list of users
     * @return a string representing the users
     */
    private String prettifySimpleUser(ArrayList<SimpleUtente> users){
        if(users.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n");
        for (SimpleUtente u : users) {
            sb.append(u.getUsername());
            for(int i = 0; i < 15 - u.getUsername().length(); i++){
                sb.append(" ");
            }
            sb.append("|| ");
            for (String tag : u.getTags()) {
                sb.append(tag);
                for(int i = 0; i < 13 - tag.length(); i++){
                    sb.append(" ");
                }
            }
            for (int i = 0; i < 5*13 - u.getTags().size() * 13; i++) {
                sb.append(" ");
            }
            sb.append("||\n");
        }
        sb.append("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\n");
        return sb.toString();
    }

    /**
    * @param p a post
    * @return a string representing the posts
    */
    private String prettifyPost(PostHead p){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 13 - p.getUsername().length()/2; i++) {
            sb.append(" ");
        }
        sb.append(p.getId());
        sb.append("\t");
        sb.append(p.getUsername());
        sb.append("\n");
        for (int i = 0; i < 15 - p.getTitle().length()/2; i++) {
            sb.append(" ");
        }
        sb.append(p.getTitle());
        sb.append("\n");
        return sb.toString();
    }
}
