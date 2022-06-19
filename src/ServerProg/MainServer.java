package ServerProg;

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
import java.util.concurrent.*;

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
                        ClientRequestRunnable task = new ClientRequestRunnable((ConnectedUser) key.attachment(), selector, exchanger, sn);
                        try{
                            workerPool.execute(task);
                        } catch (RejectedExecutionException e){
                            task.run();
                        }
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
