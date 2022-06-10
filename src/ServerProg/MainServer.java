package ServerProg;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MainServer {
    public static void main(String[] args) {
        SocialNetwork sn;
        try {
            sn = new SocialNetwork();
            activateRMI(8081, sn);
            System.out.println("Server ready");
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        ServerSocket serverSocket;
        Socket socket;
        try {
            serverSocket = new ServerSocket(8080);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":8080");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        while (true) {
            try {
                socket = serverSocket.accept();
                System.out.println("Connected to " + socket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            handleRequest(socket);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean handleRequest(Socket socket) {
        try (OutputStream oStr = socket.getOutputStream();
             BufferedReader iStr = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String request = iStr.readLine();
            System.out.println("Req  : " + request);
            oStr.write("Ciao coso\n".getBytes(StandardCharsets.UTF_8), 0, "Ciao coso\n".getBytes(StandardCharsets.UTF_8).length);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean activateRMI(int port, SocialNetwork sn) {
        try {
            Enrollment stub = (Enrollment) UnicastRemoteObject.exportObject(sn, 0);
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("WINSOME", stub);
            System.out.println("Server ready");
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
    }
}
