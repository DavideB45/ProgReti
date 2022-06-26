package ServerProg;

import ClientProg.FollowerCallback;
import ClientProg.SimpleUtente;
import ClientProg.SimpleWallet;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class Utente {
    private static final int MINUTES_TO_UPDATE_FEED = 60;
    private String username;
    private String password;
    private ArrayList<String> tags;
    private final ConcurrentArrayList<String> followers = new ConcurrentArrayList<>();
    private final ConcurrentArrayList<String> following = new ConcurrentArrayList<>();
    private final ConcurrentArrayList<Integer> posts = new ConcurrentArrayList<>();
    private final ArrayList<FollowerCallback> followersCallbacks = new ArrayList<>();
    private long lastFeedWatch;
    private final Object lockTime = new Object();
    private Wallet wallet = new Wallet();

    /**
     * @param username 2 < username.length() < 21
     * @param password 0 < password.length() < 21
     * @param tags tags representing user's interests
     */
    public Utente(String username, String password, ArrayList<String> tags){
        if(username == null || password == null || tags == null){
            throw new NullPointerException("campo mancante");
        }
        if(username.length() < 3 || username.length() > 20){
            throw new IllegalArgumentException("nome non valido");
        }
        if(password.length() > 20 || password.length() < 1){
            throw new IllegalArgumentException("password troppo lunga");
        }
        lastFeedWatch = System.currentTimeMillis() - MINUTES_TO_UPDATE_FEED * 30 * 1000;
        this.username = username;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            this.password = Arrays.toString(md.digest(password.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            this.password = password;
        }
        this.tags = new ArrayList<>();
        for (String tag : tags) {
            if(tag.length() < 20){
                this.tags.add(tag.toLowerCase());
            }
        }
    }
    public Utente(){
    }

    public void setUsername(String username){
        if(username == null){
            throw new NullPointerException("campo mancante");
        }
        if(username.length() < 3 || username.length() > 20){
            throw new IllegalArgumentException("nome non valido");
        }
        this.username = username;
    }
    public void setPassword(String password){
        if(password == null){
            throw new NullPointerException();
        }
        this.password = password;
    }
    public void setTags(ArrayList<String> tags){
        if(tags == null){
            throw new NullPointerException();
        }
        this.tags = tags;
    }
    public void setFollowers(ArrayList<String> followers){
        if(followers == null){
            throw new NullPointerException();
        }
        this.followers.clear();
        this.followers.addAll(followers);
    }
    public void setFollowing(ArrayList<String> following){
        if(following == null){
            throw new NullPointerException();
        }
        this.following.clear();
        this.following.addAll(following);
    }
    public void setPosts(ArrayList<Integer> posts){
        if(posts == null){
            throw new NullPointerException();
        }
        this.posts.clear();
        this.posts.addAll(posts);
    }
    public void setWallet(Wallet wallet){
        if(wallet == null){
            throw new NullPointerException();
        }
        this.wallet = wallet;
    }


    public String getUsername(){
        return username;
    }
    public String getPassword(){
        return password;
    }
    public ArrayList<String> getTags(){
        return tags;
    }
    public ArrayList<String> getFollowers(){
        return followers.getListCopy();
    }
    public ArrayList<String> getFollowing(){
        return following.getListCopy();
    }
    public ArrayList<Integer> getPosts(){
        return posts.getListCopy();
    }
    public SimpleWallet getWallet(){
        return wallet.copy();
    }

    /**
     * @param password user's password
     * @return true if user's password is password passed as argument
     */
    public boolean checkPassword(String password){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Arrays.toString(md.digest(password.getBytes())).equals(this.password);
        } catch (NoSuchAlgorithmException e) {
            return password.equals(this.password);
        }
    }

    /**
     * @param tags a list of interests
     * @return true if one or more tags are also this user's tags
     * @throws NullPointerException if tags is null
     */
    public boolean checkTag(ArrayList<String> tags) throws NullPointerException{
        if(tags == null){
            throw new NullPointerException("tags mancanti");
        }
        for(String tag : tags){
            if(this.tags.contains(tag)){
                return true;
            }
        }
        return false;
    }

    /**
     * add a record to user's wallet
     * @param wincoin amount of WNC earned
     * @param postId post that generated the revenue
     * @param timestamp time when WNC were earned
     */
    public void addRecord(float wincoin, int postId, long timestamp){
        wallet.addRecord(wincoin, postId, timestamp);
    }


    /**
     * add a Stub to notify for follower's info
     * @param callback the Stub to call
     * @return true if Stub was added
     */
    public synchronized boolean addCallback(FollowerCallback callback){
        if (callback == null){
            return false;
        }
        if (followersCallbacks.contains(callback)){
            return true;
        }
        return followersCallbacks.add(callback);
    }

    /**
     * remove a Stub from callback's list
     * @param callback the Stub to remove
     * @return true if Stub was removed
     */
    public synchronized boolean removeCallback(FollowerCallback callback){
        if (callback == null){
            return false;
        }
        return followersCallbacks.remove(callback);
    }

    /**
     * make a callback to every Stub registered
     * @param name the person following/unfollowing
     * @param tags person's tags
     * @param followState true if following, false if unfollowing
     */
    public synchronized void notifyAll(String name,ArrayList<String> tags,boolean followState){
        ArrayList<FollowerCallback> disconnected = new ArrayList<>();
        for(FollowerCallback callback : followersCallbacks){
            try{
                if(followState){
                    callback.newFollower(new SimpleUtente(name,tags));
                } else{
                    callback.newUnfollow(new SimpleUtente(name,tags));
                }
            } catch (RemoteException e){
                disconnected.add(callback);
            }
        }
        followersCallbacks.removeAll(disconnected);
    }

    /**
     * add follower to this
     * @param username follower
     * @return true if username wasn't this and is now following
     */
    public boolean addFollower(Utente username){
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        if(username.getUsername().equals(this.username)){
            return false;
        }
        if(followers.addIfAbsent(username.getUsername())){
            notifyAll(username.getUsername(),username.getTags(), true);
        }
        return true;
    }

    /**
     * remove username from follower list
     * @param username user unfollowing
     * @throws NullPointerException if username is null
     */
    public void removeFollower(Utente username) throws NullPointerException{
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        if(followers.removeElement(username.getUsername())){
            notifyAll(username.getUsername(), username.getTags(), false);
        }
    }

    /**
     * @return tru if username is in following list
     */
    public boolean isFollowing(String username){
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        return following.contains(username);
    }

    /**
     * this starts to follow utente
     * @param utente user to follow
     * @return true if added following
     */
    public boolean follow(Utente utente){
        synchronized (following) {
            if (utente == null) {
                throw new NullPointerException("utente mancante");
            }
            if (utente.getUsername().equals(this.username)) {
                return false;
            }
            following.addIfAbsent(utente.getUsername());
            utente.addFollower(this);
            return true;
        }
    }

    /**
     * this stops to follow utente
     * @param utente user to stop following
     */
    public void unfollow(Utente utente){
        synchronized (following) {
            if (utente == null) {
                throw new NullPointerException("utente mancante");
            }
            if (utente.getUsername().equals(this.username)) {
                return;
            }
            following.removeElement(utente.getUsername());
            utente.removeFollower(this);
        }
    }

    /**
     * add a post to the user's blog
     * @param post post's id to add
     */
    public void addPost(int post){
        posts.add(post);
    }

    /**
     * @param post post's id
     * @return true if the user has a post with the given id in blog
     */
    public boolean hasPost(int post){
        return posts.contains(post);
    }

    /**
     * add post if it is not already in the user's posts
     * @param post the id to add
     * @return true if post was added
     */
    public boolean addPostIfAbsent(int post){
        return posts.addIfAbsent(post);
    }

    /**
     * remove a post from the user's posts
     * @param post id to remove
     * @return true if post was removed
     */
    public boolean removePost(int post){
        return posts.removeElement(post);
    }

    /**
     * may update the last time user watched the feed the value is updated if
     * last request was made MINUTES_TO_UPDATE_FEED before System.currentTimeMillis()
     * @return the last time the user watched the feed
     */
    public long getLastFeedWatch(){
        synchronized (lockTime) {
            long last = lastFeedWatch;
            if (lastFeedWatch == 0) {
                lastFeedWatch = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastFeedWatch > 1000 * 60 * MINUTES_TO_UPDATE_FEED) {
                lastFeedWatch = System.currentTimeMillis();
            }
            return last;
        }
    }
    public void setLastFeedWatch(long last){
        synchronized (lockTime) {
            lastFeedWatch = last;
        }
    }

    @JsonIgnore
    public float getWincoin(){
        return wallet.getBalance();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Utente.class && ((Utente)obj).getUsername().equals(username);
    }
}
