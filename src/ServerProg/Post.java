package ServerProg;

import ClientProg.PostHead;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Post {
    private int id;
    private String creator;
    private String title;
    private String text;
    private long date;

    private HashMap<String, Integer> votes = new HashMap<>();
    private final AtomicInteger upVotes = new AtomicInteger(0);
    private final AtomicInteger downVotes = new AtomicInteger(0);

    int iterationNumber = 0;
    int oldUpVotes = 0;
    int oldDownVotes = 0;
    HashSet<String> oldComment = new HashSet<>();

    HashSet<String> curator = new HashSet<>();
    float lastWincoin = 0;

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
    public Post(){}


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCreator() {
        return creator;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public long getDate() {
        return date;
    }
    public void setDate(long date) {
        this.date = date;
    }
    public HashMap<String, Integer> getVotes() {
        return votes;
    }
    public void setVotes(HashMap<String, Integer> votes) {
        this.votes = votes;
    }
    public synchronized int getUpVotes() {
        return upVotes.get();
    }
    public synchronized void setUpVotes(int upVotes) {
        this.upVotes.set(upVotes);
    }
    public synchronized int getDownVotes() {
        return downVotes.get();
    }
    public synchronized void setDownVotes(int downVotes) {
        this.downVotes.set(downVotes);
    }
    public int getIterationNumber() {
        return iterationNumber;
    }
    public void setIterationNumber(int iterationNumber) {
        this.iterationNumber = iterationNumber;
    }
    public int getOldUpVotes() {
        return oldUpVotes;
    }
    public void setOldUpVotes(int oldUpVotes) {
        this.oldUpVotes = oldUpVotes;
    }
    public int getOldDownVotes() {
        return oldDownVotes;
    }
    public void setOldDownVotes(int oldDownVotes) {
        this.oldDownVotes = oldDownVotes;
    }
    public ArrayList<String> getOldComment() {
        return new ArrayList<>(oldComment);
    }
    public void setOldComment(ArrayList<String> oldComment) {
        this.oldComment = new HashSet<>(oldComment);
    }
    public ArrayList<Comment> getComments() {
        return comments.getListCopy();
    }
    public void setComments(ArrayList<Comment> comments) {
        this.comments.addAll(comments);
    }

    @JsonIgnore
    public PostHead getHead(){
        return new PostHead(id, creator, title);
    }
    public boolean postedAfter(long date){
        return this.date > date;
    }
    public void refreshDate(){
        this.date = System.currentTimeMillis();
    }

    public HashSet<String> calculateWincoin() {
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
        lastWincoin = (float)earnedWincoin;
        return curator;
    }
    @JsonIgnore
    public float getLastWincoin(){
        return lastWincoin;
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
            curator.add(username);
            return true;
        } else {
            return false;
        }
    }

    public void addComment(Comment comment){
        if(comment == null){
            throw new NullPointerException("null comment");
        }
        curator.add(comment.getUsername());
        comments.add(comment);
    }
    @JsonIgnore
    public Comment getComment(int i) {
        try {
            return comments.get(i);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
    @JsonIgnore
    public int getCommentCount() {
        return comments.size();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == Post.class && ((Post)obj).getId() == id;
    }
}
