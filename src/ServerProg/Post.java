package ServerProg;

import java.util.concurrent.ConcurrentHashMap;

public class Post {
    private final int id;
    private String creator;
    private String title;
    private String text;

    private ConcurrentHashMap<String, Integer> votes = new ConcurrentHashMap<String, Integer>();
    int upVotes = 0;
    int downVotes = 0;
    private ConcurrentArrayList<Comment> comments = new ConcurrentArrayList<Comment>();

    public Post(int id, String creator, String title, String text){
        if(creator == null || title == null || text == null){
            throw new NullPointerException("campo mancante");
        }
        if(title.length() > 20){
            throw new IllegalArgumentException("titolo troppo lungo");
        }
        if(text.length() > 500){
            throw new IllegalArgumentException("testo troppo lungo");
        }
        this.id = id;
        this.creator = creator;
        this.title = title;
        this.text = text;
    }
    public void deletePost(){
        title = null;
        text = null;
        creator = null;
    }

    public int getId() {
        return id;
    }
    public String getCreator() {
        return creator;
    }
    public String getTitle() {
        return title;
    }
    public String getText() {
        return text;
    }
    public int getUpVotes() {
        return upVotes;
    }
    public int getDownVotes() {
        return downVotes;
    }

    public boolean vote(String username, int vote){
        if(vote < 0){
            vote = -1;
        } else{
            vote = 1;
        }
        if(votes.putIfAbsent(username, vote) == null){
            if(vote == 1){
                upVotes++;
            } else{
                downVotes++;
            }
            return true;
        } else {
            return false;
        }
    }

    public void addComment(Comment comment){
        if(comment == null){
            throw new NullPointerException("null comment");
        }
        comments.add(comment);
    }
    public Comment getComment(int i) {
        try {
            return comments.get(i);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Post.class && ((Post)obj).getId() == id;
    }
}
