package ClientProg;

import ServerProg.Utente;

import java.io.Serializable;
import java.util.ArrayList;

public class SimpleUtente implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private ArrayList<String> tags = null;

    public SimpleUtente(String username, ArrayList<String> tags){
        this.username = username;
        this.tags = tags;
    }
    public SimpleUtente(){
    }
    public SimpleUtente(Utente utente){
        this.username = utente.getUsername();
        this.tags = utente.getTags();
    }

    public String getUsername() {
        return username;
    }
    public ArrayList<String> getTags() {
        return tags;
    }
    @Deprecated
    public void setUsername(String username) {
        this.username = username;
    }
    @Deprecated
    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleUtente that = (SimpleUtente) o;
        return username.equals(that.getUsername());
    }
}
