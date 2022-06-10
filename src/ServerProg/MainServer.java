package ServerProg;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainServer {
    public static void main(String[] args) {
        SocialNetwork sn = new SocialNetwork();
        ServerSocket serverSocket = null;
        Socket socket = null;
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
}
