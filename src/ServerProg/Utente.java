package ServerProg;

import java.util.ArrayList;

/*usare try insert sulle collezioini*/
/*deve essere concorrente*/
public class Utente {
    private final String username;
    private final String password;
    private final ArrayList<String> tags;
    private ArrayList<String> followers = new ArrayList<String>();
    private ArrayList<String> following = new ArrayList<String>();
    private ArrayList<Integer> posts = new ArrayList<Integer>();

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


    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Utente.class && ((Utente)obj).getUsername().equals(username);
    }
}
