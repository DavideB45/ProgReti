package ServerProg;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainServer {
    public static void main(String[] args) {
        SocialNetwork sn;
        ServerSocketChannel serverSocket;
        SocketChannel clientSocket;
        Selector selector;
        WncBtcCalculator exchanger = new WncBtcCalculator();
        Registry registry;
        ThreadPoolExecutor workerPool = new ThreadPoolExecutor(1, 10, 10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(15));
        try {
            sn = new SocialNetwork("users.json", "posts.json", 1);
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress(8080));
            serverSocket.configureBlocking(false);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":8080");
            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            registry = activateRMI(8081, sn);
            if (registry == null)
                return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        AtomicBoolean running = new AtomicBoolean(true);
        Thread sysInReader = new Thread(new SysInReader(running, selector));
        sysInReader.start();
        while (running.get()) {
            try {
                if (selector.select() == 0) {
                    continue;
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
            } catch (IOException e) {
                e.printStackTrace();
                workerPool.shutdownNow();
                try {
                    if(!workerPool.awaitTermination(10, TimeUnit.MINUTES)){
                        System.out.println("Thread pool not terminated");
                    }
                } catch (InterruptedException ex) {
                    e.printStackTrace();
                }
                sn.saveState();
                try {
                    selector.close();
                    serverSocket.close();
                } catch (IOException ex) {
                    return;
                }
                return;
            }
        }

        try {
            registry.unbind("WINSOME");
            UnicastRemoteObject.unexportObject(sn, true);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        workerPool.shutdown();
        try {
            if(!workerPool.awaitTermination(10, TimeUnit.MINUTES)){
                System.out.println("Thread pool not terminated");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sn.saveState();
        try {
            selector.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread x : threadSet) {
            //System.out.println(x.getName());
            if(x.getName().contains("RMI")){
                //x.interrupt();
                System.out.println("Interrupted " + x.getName());
            }
        }*/
    }

    private static Registry activateRMI(int port, SocialNetwork sn) {
        try {
            Enrollment stub = (Enrollment) UnicastRemoteObject.exportObject(sn, 0);
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind("WINSOME", stub);
            System.out.println("RMI activated");
            return registry;
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("RMI not activated");
            return null;
        }
    }
}
