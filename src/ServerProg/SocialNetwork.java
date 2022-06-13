package ServerProg;

import ClientProg.FollowerCallback;
import ClientProg.PostHead;
import ClientProg.SimplePost;
import ClientProg.SimpleUtente;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SocialNetwork implements Enrollment {
    private final ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();
    private AtomicInteger idLast = new AtomicInteger(0);

    public SocialNetwork(){
    }


    public String randomMethod() throws RemoteException {
        float random = (float) Math.random();
        System.out.println("random: " + random);
        return "random" + random;
    }

    public boolean register(String username, String password, ArrayList<String> tags) throws RemoteException {
        if(username == null || password == null || tags == null){
            throw new NullPointerException("campo mancante");
        }
        Utente u = new Utente(username, password, tags);
        System.out.println("Registrazione utente : " + username + "\npassword: " + password);
        return utenti.putIfAbsent(username, u) == null;
    }
    @Override
    public int registerCallback(FollowerCallback callback, String user, String password) throws RemoteException, NullPointerException {
        if(callback == null || user == null || password == null){
            throw new NullPointerException("missing field");
        }
        Utente u = utenti.get(user);
        if(u == null){
            return 404;
        }
        if(!u.checkPassword(password)){
            return 403;
        }
        if(u.addCallback(callback)){
            ArrayList<String> followers = u.getFollowers();
            ArrayList<SimpleUtente> followersSimple = new ArrayList<>();
            Utente utente;
            for(String follower : followers){
                utente = utenti.get(follower);
                followersSimple.add(new SimpleUtente(follower, utente.getTags()));
            }
            callback.setOldFollowers(followersSimple);
            return 200;
        } else{
            return 500;
        }
    }
    @Override
    public int unregisterCallback(FollowerCallback callback, String user, String password) throws RemoteException, NullPointerException {
        if(callback == null || user == null || password == null){
            throw new NullPointerException("missing field");
        }
        Utente u = utenti.get(user);
        if(u == null){
            return 404;
        }
        if(!u.checkPassword(password)){
            return 403;
        }
        if(u.removeCallback(callback)){
            return 200;
        } else{
            return 500;
        }
    }

    public Utente login(String username, String password){
        if(username == null || password == null){
            throw new NullPointerException("campo mancante");
        }
        System.out.println("Login utente : " + username + "\npassword: " + password);
        if(!utenti.containsKey(username) || !utenti.get(username).checkPassword(password)){
            return null;
        }
        return utenti.get(username);
    }
    public int logout(Utente user) {
        if (user == null) {
            throw new NullPointerException("campo mancante");
        }
        if (utenti.containsKey(user.getUsername())) {
            return 200;
        } else {
            return 500;
        }
    }

    public int follow(Utente user, String username) {
        if (user == null || username == null) {
            return 400;
        }
        Utente followed = utenti.get(username);
        if (followed == null) {
            return 404;
        }
        if (user.follow(followed)) {
            return 200;
        } else {
            return 500;
        }
    }
    public int unfollow(Utente user, String username) {
        if (user == null || username == null) {
            return 400;
        }
        Utente followed = utenti.get(username);
        if (followed == null) {
            return 404;
        }
        user.unfollow(followed);
        return 200;
    }
    public ArrayList<SimpleUtente> getFollowers(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<String> followers = user.getFollowers();
        ArrayList<SimpleUtente> followersSimple = new ArrayList<>();
        Utente utente;
        for (String follower : followers) {
            utente = utenti.get(follower);
            followersSimple.add(new SimpleUtente(follower, utente.getTags()));
        }
        return followersSimple;
    }
    public ArrayList<SimpleUtente> getFollowing(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<String> following = user.getFollowing();
        ArrayList<SimpleUtente> followingSimple = new ArrayList<>();
        Utente utente;
        for (String followed : following) {
            utente = utenti.get(followed);
            followingSimple.add(new SimpleUtente(followed, utente.getTags()));
        }
        return followingSimple;
    }

    public int post(Utente user, SimplePost post) {
        if (user == null || post == null) {
            return -1;
        }
        try {
            int id = idLast.incrementAndGet();
            Post p = new Post(id, user.getUsername(), post.getTitle(), post.getContent());
            posts.put(id, p);
            user.addPost(id);
            return id;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
    public Post getPost(int id) {
        return posts.get(id);
    }
    public ArrayList<PostHead> getPosts(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<PostHead> postHeads = new ArrayList<>();
        ArrayList<Integer> postIds = user.getPosts();
        for (int id : postIds) {
            postHeads.add(posts.get(id).getHead());
        }
        return postHeads;
    }
    public ArrayList<PostHead> showFeed(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<PostHead> postHeads = new ArrayList<>();
        ArrayList<String> following = user.getFollowing();
        long lastWatch = user.getLastFeedWatch();
        for (String follower : following) {
            ArrayList<Integer> postIds = utenti.get(follower).getPosts();
            for (int id : postIds) {
                Post p = posts.get(id);
                if(p != null && p.postedAfter(lastWatch)){
                    postHeads.add(p.getHead());
                }
            }
        }
        return postHeads;
    }
}
