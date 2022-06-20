package ServerProg;

import ClientProg.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SocialNetwork implements Enrollment {
    private final ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();
    private final AtomicInteger idLast = new AtomicInteger(0);
    private final Thread winCalcThread;
    private final String usersPath;
    private final String postsPath;

    public SocialNetwork(String relativePathUsers, String relativePathPosts, long winCalcThreadSleepTime) {
        JsonFactory factory = new JsonFactory();
        usersPath = relativePathUsers;
        postsPath = relativePathPosts;
        if(relativePathUsers != null) {
            File file1 = new File(relativePathUsers);
            try (JsonParser parser = factory.createParser(file1)) {
                parser.setCodec(new ObjectMapper());
                if (parser.nextToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() == JsonToken.START_OBJECT) {
                        Utente u = parser.readValueAs(Utente.class);
                        utenti.put(u.getUsername(), u);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(relativePathPosts != null) {
            File file2 = new File(relativePathPosts);
            try {
                if (file2.createNewFile()) {
                    System.out.println("posts' file created");
                }
                try (JsonParser parser = factory.createParser(file2)) {
                    parser.setCodec(new ObjectMapper());
                    if (parser.nextToken() == JsonToken.START_ARRAY){
                        parser.nextToken();
                        parser.nextToken();
                        idLast.set(parser.nextIntValue(0));
                        parser.nextToken();
                    }
                    while (parser.nextToken() == JsonToken.START_OBJECT) {
                        Post p = parser.readValueAs(Post.class);
                        posts.put(p.getId(), p);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        WincoinCalculator winC = new WincoinCalculator(1000L *60*winCalcThreadSleepTime, this.posts, this.utenti, "238.255.1.3", 3000);
        winCalcThread = new Thread(winC);
        winCalcThread.start();
    }
    public void saveState() {
        winCalcThread.interrupt();
        try {
            winCalcThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(new File(usersPath), JsonEncoding.UTF8)) {
            generator.setCodec(new ObjectMapper());
            generator.useDefaultPrettyPrinter();
            generator.writeStartArray();
            for (Utente u : utenti.values()) {
                generator.writeObject(u);
            }
            generator.writeEndArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (JsonGenerator generator = factory.createGenerator(new File(postsPath), JsonEncoding.UTF8)) {
            generator.setCodec(new ObjectMapper());
            generator.useDefaultPrettyPrinter();
            generator.writeStartArray();
            generator.writeStartObject();
            generator.writeNumberField("idLast", idLast.get());
            generator.writeEndObject();
            for (Post p : posts.values()) {
                generator.writeObject(p);
            }
            generator.writeEndArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String randomMethod() throws RemoteException {
        float random = (float) Math.random();
        System.out.println("random: " + random);
        return "random" + random;
    }

    public boolean register(String username, String password, ArrayList<String> tags) throws RemoteException {
        if(username == null || password == null || tags == null){
            throw new NullPointerException("missing parameters");
        }
        Utente u = new Utente(username, password, tags);
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
    private boolean hasPostInFeed(String username, int id){
        Utente u = utenti.get(username);
        if(u == null){
            return false;
        }
        Post p = posts.get(id);
        if(p != null && p.getCreator().equals(username)){
            return false;
        }
        ArrayList<String> following = u.getFollowing();
        for(String followingUser : following){
            Utente followingUtente = utenti.get(followingUser);
            if(followingUtente.hasPost(id)){
                return true;
            }
        }
        return false;
    }

    public Utente login(String username, String password){
        if(username == null || password == null){
            throw new NullPointerException("missing field");
        }
        System.out.println("Login utente : " + username + "\npassword: " + password);
        if(!utenti.containsKey(username) || !utenti.get(username).checkPassword(password)){
            return null;
        }
        return utenti.get(username);
    }
    public int logout(Utente user) {
        if (user == null) {
            throw new NullPointerException("missing field");
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
    public int deletePost(Utente user, int id) {
        if (user == null) {
            return 401;
        }
        Post p = posts.get(id);
        if (p == null) {
            return 404;
        } else if (p.getCreator().equals(user.getUsername())) {
            user.removePost(id);
            posts.remove(id);
            return 200;
        } else if (user.removePost(id)) {
            return 202;
        } else {
            return 403;
        }
    }
    public int rewin(Utente user, int id) {
        if (user == null) {
            return 401;
        }
        Post p = posts.get(id);
        if (p == null) {
            return 404;
        } else if (hasPostInFeed(user.getUsername(), id)) {
            if(user.addPostIfAbsent(id)){
                p.refreshDate();
                return 200;
            } else{
                return 400;
            }
        } else {
            return 403;
        }
    }
    public int ratePost(Utente user, int id, int rating) {
        if (user == null) {
            return 401;
        }
        Post p = posts.get(id);
        if (p == null) {
            return 404;
        } else if (hasPostInFeed(user.getUsername(), id)) {
            if(p.vote(user.getUsername(), rating)){
                return 200;
            } else{
                return 409;
            }
        } else {
            return 403;
        }
    }
    public int comment(Utente user, int id, String comment) {
        if (user == null || comment == null) {
            return 400;
        }
        Post p = posts.get(id);
        if (p == null) {
            return 404;
        }
        try {
            Comment c = new Comment(user.getUsername(), comment);
            if(hasPostInFeed(user.getUsername(), id)){
                p.addComment(c);
                return 200;
            } else{
                return 403;
            }
        } catch (IllegalArgumentException e) {
            return 413;
        } catch (NullPointerException e) {
            return 500;
        }
    }

    public ArrayList<PostHead> getPosts(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<PostHead> postHeads = new ArrayList<>();
        ArrayList<Integer> postIds = user.getPosts();
        for (int id : postIds) {
            Post p = posts.get(id);
            if (p != null) {
                postHeads.add(p.getHead());
            } else {
                user.removePost(id);
            }
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
                } else if (p == null) {
                    user.removePost(id);
                }
            }
        }
        return postHeads;
    }

    public SimpleWallet getWallet(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        return user.getWallet();
    }
    public float getWincoin(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        return user.getWincoin();
    }
}
