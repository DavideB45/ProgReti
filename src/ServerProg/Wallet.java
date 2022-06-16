package ServerProg;

import java.util.ArrayList;

public class Wallet {

    ConcurrentArrayList<WincoinRecord> records;
    float balance;

    public Wallet() {
        records = new ConcurrentArrayList<>();
    }
    public synchronized void addRecord(float wincoin, int postId, long timestamp) {
        records.add(new WincoinRecord(wincoin, postId, timestamp));
        balance += wincoin;
    }
    public ArrayList<WincoinRecord> getRecords() {
        return records.getListCopy();
    }
    public synchronized float getBalance() {
        return balance;
    }
}
