package ServerProg;

import ClientProg.FollowerCallback;
import ClientProg.SimpleUtente;
import ClientProg.SimpleWallet;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class Utente {
    private static final int MINUTES_TO_UPDATE_FEED = 60;
    private String username;
    private String password;
    private ArrayList<String> tags;
    private final ConcurrentArrayList<String> followers = new ConcurrentArrayList<>();
    private final ConcurrentArrayList<String> following = new ConcurrentArrayList<>();
    private ConcurrentArrayList<Integer> posts = new ConcurrentArrayList<>();
    private final ArrayList<FollowerCallback> followersCallbacks = new ArrayList<>();
    private long lastFeedWatch;
    private Wallet wallet = new Wallet();

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
        this.password = password;
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

    public boolean checkPassword(String password){
        return this.password.equals(password);
    }
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

    //functions useful to handle wallet
    public void addRecord(float wincoin, int postId, long timestamp){
        wallet.addRecord(wincoin, postId, timestamp);
    }


    // return a copy of all followers
    public synchronized boolean addCallback(FollowerCallback callback){
        if (callback == null){
            return false;
        }
        if (followersCallbacks.contains(callback)){
            return true;
        }
        return followersCallbacks.add(callback);
    }
    public synchronized boolean removeCallback(FollowerCallback callback){
        if (callback == null){
            return false;
        }
        return followersCallbacks.remove(callback);
    }
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
    public void removeFollower(Utente username) throws NullPointerException{
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        if(followers.removeElement(username.getUsername())){
            notifyAll(username.getUsername(), username.getTags(), false);
        }
    }
    public boolean isFollowing(String username){
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        return following.contains(username);
    }

    // this starts to follow utente
    public boolean follow(Utente utente){
        if(utente == null){
            throw new NullPointerException("utente mancante");
        }
        if(utente.getUsername().equals(this.username)){
            return false;
        }
        following.addIfAbsent(utente.getUsername());
        utente.addFollower(this);
        return true;
    }
    // this stops to follow utente
    public void unfollow(Utente utente){
        if(utente == null){
            throw new NullPointerException("utente mancante");
        }
        if(utente.getUsername().equals(this.username)){
            return;
        }
        following.removeElement(utente.getUsername());
        utente.removeFollower(this);
    }

    // add a post to the user's posts
    public void addPost(int post){
        posts.add(post);
    }
    // tell if the user has a post with the given id
    public boolean hasPost(int post){
        return posts.contains(post);
    }
    // add post if it is not already in the user's posts
    public boolean addPostIfAbsent(int post){
        return posts.addIfAbsent(post);
    }
    // remove a post from the user's posts
    public boolean removePost(int post){
        return posts.removeElement(post);
    }
    // return the last time the user watched the feed and eventually update it
    public long getLastFeedWatch(){
        long last = lastFeedWatch;
        if(lastFeedWatch == 0){
            lastFeedWatch = System.currentTimeMillis();
        } else if(System.currentTimeMillis() - lastFeedWatch > 1000*60*MINUTES_TO_UPDATE_FEED){
            lastFeedWatch = System.currentTimeMillis();
        }
        return last;
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
