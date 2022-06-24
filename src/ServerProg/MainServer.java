package ServerProg;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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

        File configFile;
        if(args.length == 0) {
            configFile = new File("config.txt");
        } else {
            configFile = new File(args[0]);
        }
        int registryPort = Integer.parseInt(getFromConfig(configFile, "REGISTRY_PORT", "8081"));
        String registryHost = null;
        try {
            registryHost = getFromConfig(configFile, "REGISTRY_LOCATION", "localhost");
            if(registryHost.equals("localhost")) {
                registryHost = InetAddress.getLocalHost().getHostAddress();
            }
            System.out.println("Registry host: " + registryHost);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        int minThreads = Integer.parseInt(getFromConfig(configFile, "MIN_WORKER_THREADS", "1"));
        int maxThreads = Integer.parseInt(getFromConfig(configFile, "MAX_WORKER_THREADS", "10"));
        ThreadPoolExecutor workerPool = new ThreadPoolExecutor(minThreads, maxThreads, 10,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(15));
        try {
            sn = snFromFile(configFile);
            serverSocket = ServerSocketChannel.open();
            int port = Integer.parseInt(getFromConfig(configFile, "TCP_SERVER_PORT", "8080"));
            serverSocket.socket().bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            System.out.println(InetAddress.getLocalHost().getHostAddress() + ":" + port);
            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            registry = activateRMI(registryHost, registryPort, sn);
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
                        ClientRequestRunnable task = new ClientRequestRunnable((ConnectedUser) key.attachment(), selector, exchanger, sn, registryHost, registryPort);
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

    }

    private static Registry activateRMI(String host, int port, SocialNetwork sn) {
        try {
            Enrollment stub = (Enrollment) UnicastRemoteObject.exportObject(sn, 0);
            LocateRegistry.createRegistry(port);
            Registry registry = LocateRegistry.getRegistry(host,port);
            registry.rebind("WINSOME", stub);
            System.out.println("RMI activated");
            return registry;
        } catch (RemoteException e) {
            e.printStackTrace();
            System.out.println("RMI not activated");
            return null;
        }
    }

    private static SocialNetwork snFromFile(File config) {
        String uPath = "users.json";
        String pPath = "posts.json";
        int sleepTime = 60;
        String multicastAddress = "238.255.1.3";
        int multicastPort = 8888;
        float cPercentage = 0.7f;
        if (config.canRead()){
            try {
                System.out.println("Reading config file " + config.getAbsolutePath());
                BufferedReader br = new BufferedReader(new FileReader(config));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.replaceAll(" ","").split("=");
                    if (parts[0].equals("USER_BACKUP_FILE_NAME")) {
                        uPath = parts[1];
                        //System.out.println("users " + parts[1]);
                    } else if (parts[0].equals("POST_BACKUP_FILE_NAME")) {
                        pPath = parts[1];
                        //System.out.println("posts " + parts[1]);
                    } else if (parts[0].equals("TIME_TO_CALCULATE_REWARDS")) {
                        sleepTime = Integer.parseInt(parts[1]);
                        //System.out.println("sleepTime " + parts[1]);
                    } else if (parts[0].equals("MULTICAST_GROUP")) {
                        multicastAddress = parts[1];
                        //System.out.println("multicastAddress " + parts[1]);
                    } else if (parts[0].equals("MULTICAST_PORT")) {
                        multicastPort = Integer.parseInt(parts[1]);
                        //System.out.println("multicastPort " + parts[1]);
                    } else if (parts[0].equals("CREATOR_REWARD_PERCENTAGE")) {
                        cPercentage = Float.parseFloat(parts[1]);
                        //System.out.println("cPercentage " + parts[1]);
                    }
                }
                br.close();
            } catch (IOException e) {
                System.out.println("Using default config");
            }
        } else {
            System.out.println("Using default config");
        }

        return new SocialNetwork(uPath, pPath, sleepTime, multicastAddress, multicastPort, cPercentage);
    }

    private static String getFromConfig(File config, String key, String defaultValue){
        String value = "";
        if (config.canRead()){
            try {
                BufferedReader br = new BufferedReader(new FileReader(config));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.replaceAll(" ","").split("=");
                    if (parts[0].equals(key)) {
                        value = parts[1];
                        break;
                    }
                }
                br.close();
            } catch (IOException e) {
                System.out.println("Using default config");
            }
        } else {
            System.out.println("Using default config");
        }
        if (value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
