package ClientProg;

import ServerProg.Utente;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

// potrebbero seguire 2 nuovi utenti contemporaneamente, quindi devo usare synchronized
public interface FollowerCallback extends Remote {
    void setOldFollowers(ArrayList<SimpleUtente> oldFollowers) throws RemoteException;
    void newFollower(SimpleUtente username) throws RemoteException;
    void newUnfollow(SimpleUtente username) throws RemoteException;
}