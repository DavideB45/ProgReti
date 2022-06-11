package ServerProg;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MainServer {
    public static void main(String[] args) {
        SocialNetwork sn;
        ServerSocket serverSocket;
        ConnectedUser[] users = new ConnectedUser[4];
        try {
            sn = new SocialNetwork();
            if (activateRMI(8081, sn)) {
                System.out.println("RMI activated");
            } else {
                System.out.println("RMI not activated");
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        try {
            serverSocket = new ServerSocket(8080);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":8080");
            System.out.println("Server ready");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try {
            serverSocket.setSoTimeout(10);
            System.out.println(serverSocket.getSoTimeout());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int connectedClient = 0;
        Socket socket;
        while (true) {
            try {
                socket = serverSocket.accept();
                connectedClient++;
                if (connectedClient >= users.length) {
                    errorFull(socket);
                    connectedClient--;
                } else {
                    users[connectedClient -1] = new ConnectedUser(socket, String.valueOf(connectedClient));
                    System.out.println("coso connesso\n");
                }
            } catch (SocketTimeoutException e){
                //System.out.println(connectedClient);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    return;
                }
                return;
            }
            for (int i = 0; i < connectedClient; i++) {

                if( !handleUser(users[i]) ){
                    for (int j = i; j < connectedClient; j++){
                        users[j] = users[j + 1];
                    }
                    connectedClient--;
                    break;
                }

            }
        }
    }

    private static void errorFull(Socket socket){
        try (OutputStream oStr = socket.getOutputStream()) {
            byte[] errorMessage = "Ciao coso, siamo pieni\n".getBytes(StandardCharsets.UTF_8);
            oStr.write(errorMessage, 0, errorMessage.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean handleUser(ConnectedUser u){
        if (!u.isConnected()){
            return false;
        }
        if(!u.hasRequest()){
            return true;
        }
        return u.handleRequest();
    }

    private static boolean activateRMI(int port, SocialNetwork sn) {
        try {
            Enrollment stub = (Enrollment) UnicastRemoteObject.exportObject(sn, 0);
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("WINSOME", stub);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
}
