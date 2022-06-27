package ServerProg;

import ClientProg.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SocialNetwork implements Enrollment {
    private final ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();
    private final AtomicInteger idLast = new AtomicInteger(0);
    private final Thread winCalcThread;
    private final String usersPath;
    private final String postsPath;
    private final String multicastGroup;
    private final int multicastPort;

    /**
     * create the object with given parameter
     * and populate it with info from relativePaths
     * those files are also used by saveState()
     */
    public SocialNetwork(String relativePathUsers, String relativePathPosts, long winCalcThreadSleepTime, String multicastAddress, int multicastPort, float creatorPercentage) {
        JsonFactory factory = new JsonFactory();
        usersPath = relativePathUsers;
        postsPath = relativePathPosts;
        if(relativePathUsers != null) {
            File file1 = new File(relativePathUsers);
            try {
                if (file1.createNewFile()) {
                    System.out.println("file creato");
                }
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
        this.multicastGroup = multicastAddress;
        this.multicastPort = multicastPort;
        WincoinCalculator winC = new WincoinCalculator(1000L *60*winCalcThreadSleepTime,
                this.posts, this.utenti,
                multicastAddress, multicastPort,
                creatorPercentage);
        winCalcThread = new Thread(winC);
        winCalcThread.start();
    }

    /**
     * save object state in the two files
     */
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

    /**
     * @return a string representing the multicast group used for send notification
     */
    public String getMulticastGroup() {
        return multicastGroup;
    }

    /**
     * @return multicast port to receive UDP messages
     */
    public int getMulticastPort() {
        return multicastPort;
    }

    /**
     * generate a random number
     * @return a random number
     */
    public String randomMethod() throws RemoteException {
        float random = (float) Math.random();
        System.out.println("random: " + random);
        return "random" + random;
    }

    /**
     * register a new Utente to the SocialNetwork
     * @param username unique name
     * @param password a string between 3 and 20 char
     * @param tags list of tags used
     * @return true if username is not already in use and Utente can be created
     */
    public boolean register(String username, String password, ArrayList<String> tags) throws RemoteException {
        if(username == null || password == null || tags == null){
            throw new NullPointerException("missing parameters");
        }
        Utente u = new Utente(username, password, tags);
        return utenti.putIfAbsent(username, u) == null;
    }

    /**
     * activate callback for a client
     * @param callback a Stub used to notify client about his followers
     * @param user String representing a user registered
     * @param password user's password
     * @return HTTP code representing the status of the request
     * @throws NullPointerException if a parameter was null
     */
    @Override
    public int registerCallback(FollowerCallback callback, String user, String password) throws RemoteException, NullPointerException {
        if(callback == null || user == null || password == null){
            throw new NullPointerException("missing field");
        }
        System.out.println("registerCallback" + user + " " + password + " " + callback);
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
            System.out.println("callback added");
            try {
                callback.setOldFollowers(followersSimple);
            } catch (RemoteException e) {
                e.printStackTrace();
                return 500;
            }
            System.out.println("callback set");
            return 200;
        } else{
            return 500;
        }
    }

    /**
     * remove client form callback
     * @param callback Stub to stop inform
     * @param user user's name
     * @param password user's password
     * @return HTTP code representing the status of the request
     * @throws NullPointerException if a parameter was null
     */
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

    /**
     * tell if a user has a post in his feed
     * @param username name of the user
     * @param id id of the post
     * @return true if the user has the post in his feed
     */
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

    /**
     * check if username and password allow user to login
     * @param username name of the user
     * @param password user's password
     * @return true if user is registered and password is correct
     */
    public Utente login(String username, String password){
        if(username == null || password == null){
            throw new NullPointerException("missing field");
        }
        System.out.println("Login utente : " + username + "\npassword: " + password);
        //controlli fatti separatamente perchè l'utente non può essere rimosso
        if(!utenti.containsKey(username) || !utenti.get(username).checkPassword(password)){
            return null;
        }
        return utenti.get(username);
    }

    /**
     * @param user the user that want to log out
     * @return HTTP code representing the status of the request
     */
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

    /**
     * start following username
     * @param user follower
     * @param username followed
     * @return HTTP code representing the status of the request
     */
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

    /**
     * stop following username
     * @param user follower
     * @param username followed
     * @return HTTP code representing the status of the request
     */
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

    /**
     * @param user user we are interested in
     * @return list of SimpleUtente following user
     */
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
    /**
     * @param user user we are interested in
     * @return list of SimpleUtente followed by user
     */
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
    /**
     * @param user making the request
     * @return list of SimpleUtente having at least a tag in common with user
     */
    public ArrayList<SimpleUtente> getSuggested(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<String> userTags = user.getTags();
        ArrayList<SimpleUtente> suggested = new ArrayList<>();
        for (Utente u : utenti.values()) {
            if (u.checkTag(userTags) && !u.getUsername().equals(user.getUsername())) {
                suggested.add(new SimpleUtente(u.getUsername(), u.getTags()));
            }
        }
        return suggested;
    }

    /**
     * create a new post and add it in the user's blog
     * @param user creator of the post
     * @param post the post to create
     * @return new post's id or -1 on failure
     */
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

    /**
     * @param id a post's id
     * @return the post with specified id or null if not found
     */
    public Post getPost(int id) {
        return posts.get(id);
    }

    /**
     * if user is the creator remove a post from Social network
     * else remove post from user's blog
     * @param user user that wants to delete the post
     * @param id post's id
     * @return HTTP code representing the status of the request
     */
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

    /**
     * add a post to user's blog
     * @param user user that wants to rewin the post
     * @param id post's id
     * @return HTTP code representing the status of the request
     */
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

    /**
     * add rating to a post
     * @param user user who wants to
     * @param id post to rate
     * @param rating an int representing the rating
     * @return HTTP code representing the status of the request
     */
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

    /**
     * add a comment to a post
     * @param user user commenting
     * @param id post's id
     * @param comment the comment to leave
     * @return HTTP code representing the status of the request
     */
    public int comment(Utente user, int id, String comment) {
        if (user == null || comment == null || comment.isEmpty()) {
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

    /**
     * return the blog of user
     * @param user blog's user
     * @return a list representing posts on user's blog
     */
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

    /**
     * show feed of user
     * @param user the user that want to se the feed
     * @return a list representing posts in feed
     */
    public ArrayList<PostHead> showFeed(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        ArrayList<PostHead> postHeads = new ArrayList<>();
        ArrayList<String> following = user.getFollowing();
        long lastWatch = user.getLastFeedWatch();
        HashSet<Integer> postIds = new HashSet<>();
        for (String follower : following) {
            postIds.addAll(utenti.get(follower).getPosts());
        }
        for (int id : postIds) {
            Post p = posts.get(id);
            if(p != null && p.postedAfter(lastWatch)){
                postHeads.add(p.getHead());
            }
        }
        return postHeads;
    }

    /**
     * @param user wallet's user
     * @return a copy of user's wallet
     */
    public SimpleWallet getWallet(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        return user.getWallet();
    }

    /**
     * @param user user we are interested in
     * @return the WNC amount earned by user
     */
    public float getWincoin(Utente user) {
        if (user == null) {
            throw new NullPointerException();
        }
        return user.getWincoin();
    }
}
