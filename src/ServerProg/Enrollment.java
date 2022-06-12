package ServerProg;

import ClientProg.FollowerCallback;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Enrollment extends Remote {
    boolean register(String username, String password, ArrayList<String> tags) throws RemoteException;
    int registerCallback(FollowerCallback callback, String user, String password) throws RemoteException;
    int unregisterCallback(FollowerCallback callback, String user, String password) throws RemoteException;
    String randomMethod() throws RemoteException;
}
