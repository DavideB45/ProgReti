package ClientProg;

import ServerProg.Comment;
import ServerProg.Post;

import java.util.ArrayList;
import java.util.Date;

public class SimplePost {
    private String username;
    private String title;
    private String content;
    private long date;
    private int likes;
    private int dislikes;
    private ArrayList<Comment> comments;

    public SimplePost(long timestamp, String username, String title, String content, int likes, int dislikes){
        this.date = timestamp;
        this.username = username;
        this.title = title;
        this.content = content;
        this.likes = likes;
        this.dislikes = dislikes;
        this.comments = new ArrayList<>();
    }
    public SimplePost(){
    }
    public SimplePost(String username, String title, String content){
        this(System.currentTimeMillis(), username, title, content, 0, 0);
    }
    public SimplePost(Post original){
        this.date = original.getDate();
        this.username = original.getCreator();
        this.title = original.getTitle();
        this.content = original.getText();
        this.likes = original.getUpVotes();
        this.dislikes = original.getDownVotes();
        this.comments = new ArrayList<>();
        for (int i = 0; i < original.getCommentCount(); i++) {
            this.comments.add(original.getComment(i));
        }
    }

    @Override
    public String toString() {
        if (comments != null) {
            String commentsString = "";
            for (Comment comment : comments) {
                commentsString += comment.getUsername()+ ": "+ comment.getText() + "\n";
            }
            return "post by: " + username +
                    "\n" + title +
                    "\n" + content +
                    "\n" + new Date(date) +
                    "\n↑" + likes + " ↓" + dislikes +
                    "\ncomments\n" + commentsString;
        } else {
            return "post by: " + username +
                    "\n" + title +
                    "\n" + content +
                    "\n" + new Date(date) +
                    "\n↑" + likes + " ↓" + dislikes +
                    "\nno comments\n";
        }
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
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public long getDate() {
        return date;
    }
    public void setDate(long date) {
        this.date = date;
    }
    public int getLikes() {
        return likes;
    }
    public void setLikes(int likes) {
        this.likes = likes;
    }
    public int getDislikes() {
        return dislikes;
    }
    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }
    public ArrayList<Comment> getComments() {
        return comments;
    }
    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }
}
