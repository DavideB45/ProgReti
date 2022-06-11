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
            serverSocket = new ServerSocket(8080);
            serverSocket.setSoTimeout(10);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":8080");
            System.out.println("Server ready");
            if (!activateRMI(8081, sn))
                return;
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
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
                    users[connectedClient -1] = new ConnectedUser(socket);
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

                if( !handleUser(users[i], sn) ){
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

    private static boolean handleUser(ConnectedUser u, SocialNetwork sn){
        if (!u.isConnected()){
            return false;
        }
        if(!u.hasRequest()){
            return true;
        }
        try {
            int operation = u.getOpCode();
            String[] args = u.getArguments();
            switch (operation){
                case 2:
                    Utente verifiedUser = sn.login(args[1], args[2]);
                    u.setIdentity(verifiedUser);
                    if (verifiedUser == null) {
                        System.out.println("not verified");
                        u.answer("404");
                    } else {
                        System.out.println("logged");
                        u.answer("200");
                    }
                    break;
                case 3:
                    u.answer(String.valueOf(sn.logout(u.getIdentity())));
                    break;
                default:
                    System.out.println("richiesta strana : " + operation);
                    for (String arg: args) {
                        System.out.println(arg);
                    }
                    u.answer("418");
                    break;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static boolean activateRMI(int port, SocialNetwork sn) {
        try {
            Enrollment stub = (Enrollment) UnicastRemoteObject.exportObject(sn, 0);
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("WINSOME", stub);
            System.out.println("RMI activated");
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("RMI not activated");
            return false;
        }
    }
}
