package ClientProg;

public class PostHead {
    private int id;
    private String username;
    private String title;

    public PostHead(int id, String username, String title){
        this.id = id;
        this.username = username;
        this.title = title;
    }
    public PostHead(){
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
