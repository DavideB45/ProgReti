package ClientProg;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

public class FollowerList implements FollowerCallback {
    private final HashSet<SimpleUtente> followers = new HashSet<>();
    FollowerList(){}
    @Override
    public synchronized void setOldFollowers(ArrayList<SimpleUtente> oldFollowers) throws RemoteException {
        this.followers.clear();
        this.followers.addAll(oldFollowers);
    }

    @Override
    public synchronized void newFollower(SimpleUtente username) throws RemoteException {
        this.followers.add(username);
    }

    @Override
    public synchronized void newUnfollow(SimpleUtente username) throws RemoteException {
        this.followers.remove(username);
    }

    public synchronized ArrayList<SimpleUtente> getFollowersCopy(){
        return new ArrayList<>(this.followers);
    }
    public synchronized int getSize(){
        return this.followers.size();
    }
}
