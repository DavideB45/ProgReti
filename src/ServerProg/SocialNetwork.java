package ServerProg;

import ClientProg.FollowerCallback;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class SocialNetwork implements Enrollment {
    private final ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();

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
        if(utenti.putIfAbsent(username, u) != null){
            return false;
        } else{
            return true;
        }
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
            callback.setOldFollowers(u.getFollowers());
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
}
