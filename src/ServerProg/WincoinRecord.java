package ServerProg;

public class WincoinRecord {
    private float wincoin;
    private int postId;
    private long timestamp;

    public WincoinRecord(float wincoin, int postId, long timestamp) {
        this.wincoin = wincoin;
        this.postId = postId;
        this.timestamp = timestamp;
    }

    public float getWincoin() {
        return wincoin;
    }
    public void setWincoin(float wincoin) {
        this.wincoin = wincoin;
    }
    public int getPostId() {
        return postId;
    }
    public void setPostId(int postId) {
        this.postId = postId;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
