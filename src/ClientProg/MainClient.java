package ClientProg;

import java.io.*;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.util.ArrayList;

public class MainClient {
    public static void main(String[] args){
        ServerConnection serverConn;
        int portTCP = 8080;
        String host = "192.168.56.1";
        try {
            serverConn = new ServerConnection(InetAddress.getByName(host), portTCP);
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
                        answer = serverConn.logout();
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
                        switch (splitReq[1]) {
                            case "post":
                                answer = serverConn.showPost(splitReq[2]);
                                break;
                            case "feed":
                                answer = serverConn.showFeed();
                                break;
                            case "blog":
                                answer = "scrivi solo blog";
                                break;
                            default:
                                answer = "operazione non riconosciuta";
                                break;
                        };
                        break;
                    case "list":
                        switch (splitReq[1]) {
                            case "following":
                                answer = serverConn.listFollowing();
                                break;
                            case "followers":
                                answer = serverConn.listFollowers();
                                break;
                            case "users":
                                answer = serverConn.listUsers();
                                break;
                            default:
                                answer = "operazione non riconosciuta";
                                break;
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
