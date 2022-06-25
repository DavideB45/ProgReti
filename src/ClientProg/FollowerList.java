package ClientProg;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

public class FollowerList implements FollowerCallback {
    private final HashSet<SimpleUtente> followers = new HashSet<>();
    FollowerList(){}
    /**
     * add old followers to the collection
     * @param oldFollowers people already following
     */
    @Override
    public synchronized void setOldFollowers(ArrayList<SimpleUtente> oldFollowers) throws RemoteException {
        this.followers.clear();
        this.followers.addAll(oldFollowers);
    }
    /**
     * add Simple utente in this collection
     * @param username user to add
     */
    @Override
    public synchronized void newFollower(SimpleUtente username) throws RemoteException {
        this.followers.add(username);
    }
    /**
     * remove Simple utente in this collection
     * @param username user to remove
     */
    @Override
    public synchronized void newUnfollow(SimpleUtente username) throws RemoteException {
        this.followers.remove(username);
    }

    /**
     * @return a shallow copy of this collection
     */
    public synchronized ArrayList<SimpleUtente> getFollowersCopy(){
        return new ArrayList<>(this.followers);
    }
    public synchronized int getSize(){
        return this.followers.size();
    }
}
