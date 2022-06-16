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
                        ArrayList<String> tags = new ArrayList<>();
                        for (int i = 3; i < splitReq.length; i++) {
                            tags.add(splitReq[i]);
                        }
                        answer = serverConn.register(splitReq[1], splitReq[2], tags);
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
                    case "post":
                        answer = serverConn.post(splitReq[1], splitReq[2]);
                        break;
                    case "show":
                        answer = switch (splitReq[1]) {
                            case "post" -> serverConn.showPost(splitReq[2]);
                            case "feed" -> serverConn.showFeed();
                            case "blog" -> "scrivi solo blog";
                            default -> "operazione non riconosciuta";
                        };
                        break;
                    case "list":
                        answer = switch (splitReq[1]) {
                            case "following" -> serverConn.listFollowing();
                            case "followers" -> serverConn.listFollowers();
                            default -> "operazione non riconosciuta";
                        };
                        break;
                    case "blog":
                        answer = serverConn.viewBlog();
                        break;
                    case "delete":
                        answer = serverConn.deletePost(splitReq[1]);
                        break;
                    case "rewin":
                        answer = serverConn.rewin(splitReq[1]);
                        break;
                    case "rate":
                        answer = serverConn.rate(splitReq[1], splitReq[2]);
                        break;
                    case "comment":
                        answer = serverConn.comment(splitReq[1], splitReq[2]);
                        break;
                    case "wallet":
                        if (splitReq.length == 1) {
                            answer = serverConn.wallet();
                        } else {
                            //answer = serverConn.walletBTC();
                            answer = "operazione non implementata";
                        }
                        break;
                    case "exit":
                        // TODO: implementare chiusura pulita per ServerConnection
                        return;
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
