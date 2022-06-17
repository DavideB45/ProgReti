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
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MainServer {
    public static void main(String[] args) {
        SocialNetwork sn;
        ServerSocket serverSocket;
        ConnectedUser[] users = new ConnectedUser[4];
        WncBtcCalculator exchanger = new WncBtcCalculator();
        try {
            sn = new SocialNetwork();
            serverSocket = new ServerSocket(8080);
            serverSocket.setSoTimeout(10);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":8080");
            System.out.println("Server ready");
            if (!activateRMI(8081, sn))
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

                if( !handleUser(users[i], sn, exchanger) ){
                    System.out.println("uno disconnesso");
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
            byte[] errorMessage = "Ciao coso, siamo pieni\n\n".getBytes(StandardCharsets.UTF_8);
            oStr.write(errorMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
