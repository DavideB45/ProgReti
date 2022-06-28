package ServerProg;

import ClientProg.PostHead;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Post {
    private int id;
    private String creator;
    private String title;
    private String text;
    private final AtomicLong date = new AtomicLong(0);

    private HashMap<String, Integer> votes = new HashMap<>();
    private int upVotes = 0;
    private int downVotes = 0;

    int iterationNumber = 0;
    int oldUpVotes = 0;
    int oldDownVotes = 0;
    HashSet<String> oldComment = new HashSet<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    HashSet<String> curator = new HashSet<>();
    float lastWincoin = 0;

    private final ArrayList<Comment> comments = new ArrayList<>();

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
        this.date.set(System.currentTimeMillis());
    }
    public Post(){}


    public int getId() {
        readLock.lock();
        try {
            return id;
        } finally {
            readLock.unlock();
        }
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCreator() {
        readLock.lock();
        try {
            return creator;
        } finally {
            readLock.unlock();
        }
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }
    public String getTitle() {
        readLock.lock();
        try {
            return title;
        } finally {
            readLock.unlock();
        }
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getText() {
        readLock.lock();
        try {
            return text;
        } finally {
            readLock.unlock();
        }
    }
    public void setText(String text) {
        this.text = text;
    }
    public long getDate() {
        return date.get();
    }
    public void setDate(long date) {
        this.date.set(date);
    }
    public HashMap<String, Integer> getVotes() {
        return votes;
    }
    public void setVotes(HashMap<String, Integer> votes) {
        this.votes = votes;
    }
    public int getUpVotes() {
        readLock.lock();
        try {
            return upVotes;
        } finally {
            readLock.unlock();
        }
    }
    public void setUpVotes(int upVotes) {
        this.upVotes = upVotes;
    }
    public int getDownVotes() {
        readLock.lock();
        try {
            return downVotes;
        } finally {
            readLock.unlock();
        }
    }
    public void setDownVotes(int downVotes) {
        this.downVotes = downVotes;
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
        return comments;
    }
    public void setComments(ArrayList<Comment> comments) {
        this.comments.addAll(comments);
    }

    /**
    * return a simplified version of the post
    * containing only id creator and title
    */
    @JsonIgnore
    public PostHead getHead(){
        readLock.lock();
        try {
            return new PostHead(id, creator, title);
        } finally {
            readLock.unlock();
        }
    }
    /**
    * return true if the Post has been posted after date,
    * or it has been rewinded after date
    * return false otherwise
    */
    public boolean postedAfter(long date){
        return true;
        //return this.date.get() > date;
    }
    /**
     * set the date of the post to System.currentTimeMillis()
     */
    public void refreshDate(){
        this.date.set(System.currentTimeMillis());
    }

    /**
     * method to calculate the wincoin earned during the iteration.
     * returns the list of user that interacted with the post
     * to obtain the value of earned wincoin see getLastWincoin()
     * not safe to call the method by 2 or more threads
     */
    public HashSet<String> calculateWincoin() {
        /*function use the read lock because the only variables it modifies
        only by this method*/
        readLock.lock();
        try {
            iterationNumber++;
            //create a list of new commenting user
            HashMap<String, Integer> commentators = new HashMap<>();
            for (Comment c : comments) {
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
            // calculate the wincoin earned by comments
            for (Integer i : commentators.values()) {
                earnedWincoin += 2 / (1 + Math.pow(Math.E, -(i - 1)));
            }
            earnedWincoin = Math.log(earnedWincoin + 1) / iterationNumber;
            int newUpVotes = upVotes - oldUpVotes;
            int newDownVotes = downVotes - oldDownVotes;
            oldUpVotes = upVotes;
            oldDownVotes = downVotes;
            // calculate the wincoin earned by votes
            earnedWincoin += Math.log(Math.max(newUpVotes - newDownVotes, 0) + 1) / iterationNumber;
            lastWincoin = (float) earnedWincoin;
            return new HashSet<>(curator);
        } finally {
            readLock.unlock();
        }
    }
    @JsonIgnore
    public float getLastWincoin(){
        return lastWincoin;
    }
    /**
    * add the rating of the username
    * method can be called once for every user
    * if vote < 0 rating is negative
    * positive if >= 0
    * return false if post already been rated by username
    */
    public boolean vote(String username, int vote){
        writeLock.lock();
        try {
            if (vote < 0) {
                vote = -1;
            } else {
                vote = 1;
            }
            if (votes.putIfAbsent(username, vote) == null) {
                if (vote == 1) {
                    upVotes++;
                } else {
                    downVotes++;
                }
                curator.add(username);
                return true;
            } else {
                return false;
            }
        } finally {
            writeLock.unlock();
        }
    }
    /**
     * add comment to the post
     */
    public void addComment(Comment comment){
        writeLock.lock();
        try{
            if(comment == null){
                throw new NullPointerException("null comment");
            }
            curator.add(comment.getUsername());
            comments.add(comment);
        }finally{
            writeLock.unlock();
        }
    }
    /**
     * return the comment in position i
     */
    @JsonIgnore
    public Comment getComment(int i) {
        readLock.lock();
        try {
            try {
                return comments.get(i);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }
    /**
     * return the number of comments to the post
     */
    @JsonIgnore
    public int getCommentCount() {
        readLock.lock();
        try {
            return comments.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean equals(Object obj) {
        readLock.lock();
        try{
            return obj.getClass() == Post.class && ((Post)obj).getId() == id;
        }finally{
            readLock.unlock();
        }
    }
}
