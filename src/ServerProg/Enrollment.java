package ServerProg;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Enrollment extends Remote {
    public boolean register(String username, String password, ArrayList<String> tags) throws RemoteException;
    public String randomMethod() throws RemoteException;
}
