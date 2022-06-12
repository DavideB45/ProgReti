package ClientProg;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class FollowerList implements FollowerCallback {
    private final ArrayList<String> followers;
    FollowerList(){
        this.followers = new ArrayList<>();
    }

    @Override
    public synchronized void setOldFollowers(ArrayList<String> oldFollowers) throws RemoteException {
        this.followers.clear();
        this.followers.addAll(oldFollowers);
    }

    @Override
    public synchronized void newFollower(String username) throws RemoteException {
        this.followers.add(username);
        System.out.println("new follower: " + username);
    }

    @Override
    public synchronized void newUnfollow(String username) throws RemoteException {
        this.followers.remove(username);
        System.out.println("lost follower: " + username);
    }

    public synchronized ArrayList<String> getFollowersCopy(){
        return new ArrayList<>(this.followers);
    }
    public synchronized int getSize(){
        return this.followers.size();
    }
}
