package ServerProg;

public class Comment {
    private String username;
    private String text;

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

    public synchronized void setUsername(String username) {
        this.username = username;
    }

    public synchronized void setText(String text) {
        this.text = text;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getText() {
        return text;
    }
}
