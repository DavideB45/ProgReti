package ClientProg;

import ServerProg.Utente;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Enrollment extends Remote {
    public Utente register(String username, String password, ArrayList<String> tags) throws RemoteException;
    public String randomMethod() throws RemoteException;
}
