package ServerProg;

import ClientProg.PostHead;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Post {
    private final int id;
    private final String creator;
    private final String title;
    private final String text;
    private long date;

    private final HashMap<String, Integer> votes = new HashMap<>();
    private AtomicInteger upVotes = new AtomicInteger(0);
    private AtomicInteger downVotes = new AtomicInteger(0);

    int iterationNumber = 0;
    int oldUpVotes = 0;
    int oldDownVotes = 0;
    HashSet<String> oldComment = new HashSet<>();

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

    public float calculateWincoin() {
        iterationNumber++;
        ArrayList<Comment> commentsCopy = this.comments.getListCopy();
        HashMap<String, Integer> commentators = new HashMap<>();
        for(Comment c : commentsCopy) {
            if (!oldComment.contains(c.getUsername())) {
                if (!commentators.containsKey(c.getUsername())) {
                    commentators.put(c.getUsername(), 1);
                } else {
                    commentators.put(c.getUsername(), commentators.get(c.getUsername()) + 1);
                }
            }
        }
        oldComment.addAll(commentators.keySet());
        double earnedWincoin = 0;
        for(Integer i : commentators.values()){
            earnedWincoin += 2/( 1 + Math.pow(Math.E, -(i-1)) );
        }
        earnedWincoin = Math.log(earnedWincoin + 1)/iterationNumber;
        int upV = upVotes.get();
        int downV = downVotes.get();
        int newUpVotes = upV - oldUpVotes;
        int newDownVotes = downV - oldDownVotes;
        oldUpVotes = upV;
        oldDownVotes = downV;
        earnedWincoin += Math.log(Math.max(newUpVotes - newDownVotes, 0) + 1)/iterationNumber;
        return (float) earnedWincoin;
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
    public void refreshDate(){
        this.date = System.currentTimeMillis();
    }

    public synchronized int getUpVotes() {
        return upVotes.get();
    }
    public synchronized int getDownVotes() {
        return downVotes.get();
    }
    public synchronized boolean vote(String username, int vote){
        if(vote < 0){
            vote = -1;
        } else{
            vote = 1;
        }
        if(votes.putIfAbsent(username, vote) == null){
            if(vote == 1){
                upVotes.incrementAndGet();
            } else{
                downVotes.incrementAndGet();
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
