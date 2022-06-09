package ServerProg;

public class Comment {
    private final String username;
    private final String text;

    public Comment(String username, String text){
        if(username == null || text == null){
            throw new NullPointerException("campo mancante");
        }
        if(text.length() > 60){
            throw new IllegalArgumentException("testo troppo lungo");
        }
        this.username = username;
        this.text = text;
    }
}
