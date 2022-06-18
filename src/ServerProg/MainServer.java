package ServerProg;


import ClientProg.PostHead;
import ClientProg.SimplePost;
import ClientProg.SimpleUtente;
import ClientProg.SimpleWallet;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainServer {
    public static void main(String[] args) {
        SocialNetwork sn;
        ServerSocketChannel serverSocket;
        SocketChannel clientSocket;
        Selector selector;
        int connectedClient = 0;
        WncBtcCalculator exchanger = new WncBtcCalculator();
        ThreadPoolExecutor workerPool = new ThreadPoolExecutor(1, 10, 10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(15));
        try {
            sn = new SocialNetwork();
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(8080));
            serverSocket.configureBlocking(false);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":8080");
            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            if (!activateRMI(8081, sn))
                return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                if (selector.select() == 0) {
                    System.out.println("waken");
                }
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isAcceptable()) {
                        clientSocket = serverSocket.accept();
                        clientSocket.configureBlocking(false);
                        SelectionKey newKey = clientSocket.register(selector, SelectionKey.OP_READ);
                        ConnectedUser newUser = new ConnectedUser(clientSocket, newKey);
                        newKey.attach(newUser);
                        connectedClient++;
                    } else if(key.isWritable() || key.isReadable()) {
                        // add thread to handle request
                        key.interestOps(0);
                        workerPool.execute(new ClientRequestRunnable((ConnectedUser) key.attachment(), selector, exchanger, sn));
                    }
                }

            } catch (SocketTimeoutException e){
                //System.out.println(connectedClient);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    selector.close();
                    serverSocket.close();
                } catch (IOException ex) {
                    return;
                }
                return;
            }
        }
    }

    private static void errorFull(Socket socket){
        try (OutputStream oStr = socket.getOutputStream()) {
            byte[] errorMessage = "Ciao coso, siamo pieni\n\n".getBytes(StandardCharsets.UTF_8);
            oStr.write(errorMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/* old function
    private static boolean handleUser(ConnectedUser u, SocialNetwork sn, WncBtcCalculator exchange){
        if (!u.isConnected()){
            return false;
        }
        if(!u.hasRequest()){
            return true;
        }
        try {
            int operation = u.getOpCode();
            String[] args = u.getArguments();
            Utente user = u.getIdentity();
            ObjectMapper mapper = new ObjectMapper();
            switch (operation){
                case 2:
                    Utente verifiedUser = sn.login(args[0], args[1]);
                    u.setIdentity(verifiedUser);
                    if (verifiedUser == null) {
                        u.answer("404\n\n");
                    } else {
                        u.answer("200\n238.255.1.3\n3000\n\n");
                    }
                    break;
                case 3:
                    u.answer(sn.logout(user) + "\n\n");
                    u.setIdentity(null);
                    break;
                case 7:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        u.answer(sn.follow(user, args[0]) + "\n\n");
                    }
                    break;
                case 6:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        ArrayList<SimpleUtente> list = sn.getFollowing(user);
                        if (list == null) {
                            u.answer("404\n\n");
                        } else {
                            String jsonList = mapper.writeValueAsString(list);
                            u.answer(200 +"\n"+ jsonList + "\n\n");
                        }
                    }
                    break;
                case 8:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        u.answer(sn.unfollow(user, args[0]) + "\n\n");
                    }
                    break;
                case 9:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        ArrayList<PostHead> posts = sn.getPosts(user);
                        if (posts == null) {
                            u.answer("404\n\n");
                        } else {
                            String jsonList = mapper.writeValueAsString(posts);
                            u.answer(200 +"\n"+ jsonList + "\n\n");
                        }
                    }
                    break;
                case 10:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        SimplePost post = mapper.readValue(args[0], SimplePost.class);
                        int num = sn.post(user,post);
                        if (num == -1) {
                            u.answer("400\n\n");
                        } else {
                            u.answer("200\n"+ num + "\n\n");
                        }
                    }
                    break;
                case 11:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        ArrayList<PostHead> posts = sn.showFeed(user);
                        if (posts == null) {
                            u.answer("500\n\n");
                        } else {
                            String jsonList = mapper.writeValueAsString(posts);
                            u.answer(200 +"\n"+ jsonList + "\n\n");
                        }
                    }
                    break;
                case 12:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        Post post = sn.getPost(Integer.parseInt(args[0]));
                        if (post == null) {
                            u.answer("404\n\n");
                        } else {
                            u.answer("200\n" + mapper.writeValueAsString(new SimplePost(post)) + "\n\n");
                        }
                    }
                    break;
                case 13:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        u.answer(sn.deletePost(user, Integer.parseInt(args[0])) + "\n\n");
                    }
                    break;
                case 14:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        u.answer(sn.rewin(user, Integer.parseInt(args[0])) + "\n\n");
                    }
                    break;
                case 15:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        u.answer(sn.ratePost(user, Integer.parseInt(args[0]), Integer.parseInt(args[1])) + "\n\n");
                    }
                    break;
                case 16:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        u.answer(sn.comment(user, Integer.parseInt(args[0]), args[1]) + "\n\n");
                    }
                    break;
                case 17:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        SimpleWallet wallet = sn.getWallet(user);
                        if (wallet == null) {
                            u.answer("500\n\n");
                        } else {
                            System.out.println(mapper.writeValueAsString(wallet));
                            u.answer("200\n" + mapper.writeValueAsString(wallet) + "\n\n");
                        }
                    }
                    break;
                case 18:
                    if(user == null){
                        u.answer("401\n\n");
                    } else {
                        float btc = sn.getWincoin(user);
                        if (btc >= 0) {
                            btc = exchange.WNCtoBTC(btc);
                            if (btc == -1)
                                u.answer("503\n\n");
                            else
                                u.answer("200\n" + btc + "\n\n");
                        }
                        else
                            u.answer("500\n\n");
                    }
                    break;
                default:
                    System.out.println("richiesta strana : " + operation);
                    for (String arg: args) {
                        System.out.println(arg);
                    }
                    u.answer("418\n\n");
                    break;
            }
        } catch (JsonMappingException | JsonParseException e) {
            e.printStackTrace();
            try {
                u.answer("400\n\n");
            } catch (IOException ex) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (NumberFormatException e){
            e.printStackTrace();
            try {
                u.answer("400\n\n");
            } catch (IOException ex) {
                return false;
            }
        }
        return true;
    }
 */

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
