package ServerProg;

import java.util.ArrayList;

public class Utente {
    private final String username;
    private final String password;
    private final ArrayList<String> tags;
    private final ConcurrentArrayList<String> followers = new ConcurrentArrayList<>();
    private final ConcurrentArrayList<String> following = new ConcurrentArrayList<>();
    private final ConcurrentArrayList<Integer> posts = new ConcurrentArrayList<>();

    public Utente(String username, String password, ArrayList<String> tags){
        if(username == null || password == null || tags == null){
            throw new NullPointerException("campo mancante");
        }
        if(username.length() < 3 || username.length() > 20){
            throw new IllegalArgumentException("nome non valido");
        }
        if(password.length() > 20){
            throw new IllegalArgumentException("password troppo lunga");
        }
        this.username = username;
        this.password = password;
        this.tags = tags;
    }

    public String getUsername(){
        return username;
    }
    public boolean checkPassword(String password){
        return this.password.equals(password);
    }
    public ArrayList<String> getTags(){
        return tags;
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

    // return a copy of all followers
    public ArrayList<String> getFollowers(){
        return followers.getListCopy();
    }
    public boolean addFollower(String username){
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        if(username.equals(this.username)){
            return false;
        }
        return following.addIfAbsent(username);
    }
    public void removeFollower(String username) throws NullPointerException{
        if(username == null){
            throw new NullPointerException("username mancante");
        }
        following.remove(username);
    }

    // return a copy of all following
    public ArrayList<String> getFollowing(){
        return following.getListCopy();
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
        utente.addFollower(this.username);
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
        following.remove(utente.getUsername());
        utente.removeFollower(this.username);
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Utente.class && ((Utente)obj).getUsername().equals(username);
    }
}
