package ServerProg;

import ClientProg.PostHead;

import java.util.HashMap;

public class Post {
    private final int id;
    private final String creator;
    private final String title;
    private final String text;
    private final long date;

    private final HashMap<String, Integer> votes = new HashMap<>();
    private int upVotes = 0;
    private int downVotes = 0;

    private final ConcurrentArrayList<Comment> comments = new ConcurrentArrayList<>();

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
        this.date = System.currentTimeMillis();
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
    public long getDate() {
        return date;
    }
    public PostHead getHead(){
        return new PostHead(id, creator, title);
    }
    public boolean postedAfter(long date){
        return this.date > date;
    }

    public synchronized int getUpVotes() {
        return upVotes;
    }
    public synchronized int getDownVotes() {
        return downVotes;
    }
    public synchronized boolean vote(String username, int vote){
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
    public int getCommentCount() {
        return comments.size();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Post.class && ((Post)obj).getId() == id;
    }
}
