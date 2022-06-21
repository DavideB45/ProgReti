package ClientProg;

import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.util.ArrayList;

public class MainClient {
    public static void main(String[] args){
        ServerConnection serverConn;
        int portTCP = 3030;
        try {
            serverConn = new ServerConnection(InetAddress.getLocalHost(), portTCP);
        } catch (IOException | NotBoundException e) {
            System.out.println("Impossibile connettersi al server");
            System.out.println("porta :" + portTCP);
            return;
        }

        String request;
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String answer;
        while (true) {
            try {
                request = stdIn.readLine();
                String[] splitReq = request.split(" ");
                switch (splitReq[0]) {
                    case "register":
                        ArrayList<String> tags = new ArrayList<>();
                        for (int i = 0; i < splitReq.length - 3 && i < 5; i++) {
                            tags.add(splitReq[i + 3]);
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
                        String[] post = request.split("\"");
                        answer = serverConn.post(post[1], post[3]);
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
                            case "users" -> serverConn.listUsers();
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
                        String[] comment = request.split("\"");
                        answer = serverConn.comment(splitReq[1], comment[1]);
                        break;
                    case "wallet":
                        if (splitReq.length == 1) {
                            answer = serverConn.wallet();
                        } else if (splitReq[1].equals("btc")) {
                            answer = serverConn.getWalletInBitcoin();
                        } else {
                            answer = "operazione non riconosciuta";
                        }
                        break;
                    case "reconnect":
                        if(serverConn.reconnect()){
                            answer = "connessione riattivata";
                        } else {
                            answer = "connessione non riattivata";
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
            } catch (SocketException e) {
                System.out.println("Connessione con server persa");
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ArrayIndexOutOfBoundsException e){
                System.out.println("non hai inserito tutti i parametri");
            } catch (NullPointerException e){
                System.out.println("Null pointer | probabilmente server non disponibile");
            }
        }
    }
}
