package ServerProg;

import ClientProg.SimpleWallet;

import java.util.ArrayList;

public class Wallet {

    private ConcurrentArrayList<WincoinRecord> wincoinRecords;
    private float balance;

    public Wallet() {
        wincoinRecords = new ConcurrentArrayList<>();
    }
    public synchronized void addRecord(float wincoin, int postId, long timestamp) {
        wincoinRecords.add(new WincoinRecord(wincoin, postId, timestamp));
        balance += wincoin;
    }
    public ArrayList<WincoinRecord> getWincoinRecords() {
        return wincoinRecords.getListCopy();
    }
    public synchronized float getBalance() {
        return balance;
    }

    public synchronized void setBalance(float balance) {
        this.balance = balance;
    }
    public synchronized void setWincoinRecords(ArrayList<WincoinRecord> wincoinRecords) {
        this.wincoinRecords = new ConcurrentArrayList<>(wincoinRecords);
    }
    public synchronized SimpleWallet copy() {
        SimpleWallet copy = new SimpleWallet();
        copy.setBalance(balance);
        copy.setWincoinRecords(wincoinRecords.getListCopy());
        return copy;
    }
    public synchronized String toString() {
        return "WALLET\n" +
                "wincoinRecords\n" + wincoinRecords +
                "balance: " + balance;
    }
}
