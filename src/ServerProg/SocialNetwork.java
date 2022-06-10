package ServerProg;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class SocialNetwork implements Enrollment {
    private final ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();

    public SocialNetwork() throws RemoteException {
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
        if(utenti.putIfAbsent(username, u) != null){
            throw new IllegalArgumentException("username già in uso");
        } else{
            return true;
        }
    }

    public Utente login(String username, String password){
        if(username == null || password == null){
            throw new NullPointerException("campo mancante");
        }
        if(!utenti.containsKey(username) || !utenti.get(username).checkPassword(password)){
            return null;
        }
        return utenti.get(username);
    }

    public boolean logout(String username, Utente user){
        if(username == null){
            throw new NullPointerException("campo mancante");
        }
        return utenti.containsKey(username);
    }
}
