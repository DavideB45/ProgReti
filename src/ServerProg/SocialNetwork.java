package ServerProg;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class SocialNetwork {
    private final ConcurrentHashMap<String, Utente> utenti = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Post> posts = new ConcurrentHashMap<>();

    public SocialNetwork(){
    }

    public Utente register(String username, String password, ArrayList<String> tags){
        if(username == null || password == null || tags == null){
            throw new NullPointerException("campo mancante");
        }
        Utente u = new Utente(username, password, tags);
        if(utenti.putIfAbsent(username, u) != null){
            throw new IllegalArgumentException("username già in uso");
        } else{
            return u;
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
