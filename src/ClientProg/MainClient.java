package ClientProg;

import java.io.*;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.util.ArrayList;

public class MainClient {
    public static void main(String[] args){
        ServerConnection serverConn;
        try {
            serverConn = new ServerConnection(InetAddress.getLocalHost(), 8080);
        } catch (IOException | NotBoundException e) {
            e.printStackTrace();
            return;
        }

        String request;
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String answer;
        while (true) {
            try {
                request = stdIn.readLine();
                String[] splitReq = request.split(" ");
                switch (splitReq[0]){
                    case "register":
                        answer = serverConn.register(splitReq[1], splitReq[2], new ArrayList<>());
                        break;
                    case "login":
                        answer = serverConn.login(splitReq[1], splitReq[2]);
                        break;
                    case "logout":
                        answer = serverConn.logout(splitReq[1]);
                        break;
                    case "follow":
                        answer = serverConn.follow(splitReq[1]);
                        break;
                    case "unfollow":
                        answer = serverConn.unfollow(splitReq[1]);
                        break;
                    default:
                        answer = "operazione non riconosciuta";
                        break;
                }
                System.out.println(answer);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e){
                System.out.println("non hai inserito tutti i parametri");
            }
        }
    }
}
