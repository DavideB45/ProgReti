package ClientProg;

import ServerProg.WincoinRecord;

import java.util.ArrayList;

public class SimpleWallet {
    private ArrayList<WincoinRecord> wincoinRecords;
    private float balance;

    public SimpleWallet() {}

    public ArrayList<WincoinRecord> getWincoinRecords() {
        return wincoinRecords;
    }
    public void setWincoinRecords(ArrayList<WincoinRecord> wincoinRecords) {
        this.wincoinRecords = wincoinRecords;
    }
    public float getBalance() {
        return balance;
    }
    public void setBalance(float balance) {
        this.balance = balance;
    }
    public void addRecord(WincoinRecord record) {
        wincoinRecords.add(record);
    }

    @Override
    public String toString() {
        StringBuilder recordsString = new StringBuilder();
        for (WincoinRecord record : wincoinRecords) {
            recordsString.append(record.getPostId()).append(": ").append(record.getWincoin()).append(": ").append(record.getTimestamp()).append("\n");
        }
        return "WALLET\n" +
                "wincoinRecords\n" + recordsString.toString() +
                "balance: " + balance;
    }
}
