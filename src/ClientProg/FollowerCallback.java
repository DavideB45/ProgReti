package ClientProg;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

// potrebbero seguire 2 nuovi utenti contemporaneamente, quindi devo usare synchronized
public interface FollowerCallback extends Remote {
    void setOldFollowers(ArrayList<String> oldFollowers) throws RemoteException;
    void newFollower(String username) throws RemoteException;
    void newUnfollow(String username) throws RemoteException;
}