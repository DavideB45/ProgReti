package ClientProg;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class FollowerList implements FollowerCallback {
    private final ArrayList<SimpleUtente> followers = new ArrayList<>();
    FollowerList(){}

    @Override
    public synchronized void setOldFollowers(ArrayList<SimpleUtente> oldFollowers) throws RemoteException {
        this.followers.clear();
        this.followers.addAll(oldFollowers);
    }

    @Override
    public synchronized void newFollower(SimpleUtente username) throws RemoteException {
        this.followers.add(username);
        System.out.println("new follower: " + username.getUsername());
    }

    @Override
    public synchronized void newUnfollow(SimpleUtente username) throws RemoteException {
        this.followers.remove(username);
        System.out.println("lost follower: " + username.getUsername());
    }

    public synchronized ArrayList<SimpleUtente> getFollowersCopy(){
        return new ArrayList<>(this.followers);
    }
    public synchronized int getSize(){
        return this.followers.size();
    }
}
