package ServerProg;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class WincoinCalculator implements Runnable {
    private final long iterationsInterleave;
    private final ConcurrentHashMap<Integer, Post> posts;
    private final ConcurrentHashMap<String, Utente> utenti;
    private InetAddress multicastAddress;
    private final int multicastPort;
    private final float creatorPercentage;

    public WincoinCalculator(long iterationsInterleave, ConcurrentHashMap<Integer, Post> posts, ConcurrentHashMap<String, Utente> utenti, String multicastAddress, int multicastPort, float creatorPercentage) {
        this.iterationsInterleave = iterationsInterleave;
        this.posts = posts;
        this.utenti = utenti;
        this.multicastPort = multicastPort;
        if(creatorPercentage < 0.5 || creatorPercentage > 1){
            this.creatorPercentage = 0.5f;
        } else {
            this.creatorPercentage = creatorPercentage;
        }
        try {
            this.multicastAddress = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            this.multicastAddress = null;
        }
    }

    @Override
    public void run() {
        long lastIteration = System.currentTimeMillis();
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("Errore nella creazione del socket");
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(iterationsInterleave - (System.currentTimeMillis() - lastIteration));
            } catch (InterruptedException e) {
                if (socket != null) {
                    socket.close();
                }
                break;
            }
            HashMap<String, Float> curatorEarnings = new HashMap<>();
            for (Post post : posts.values()) {
                HashSet<String> postCurators = post.calculateWincoin();
                float wincoin = post.getLastWincoin();
                if (wincoin > 0) {
                    Utente creator = utenti.get(post.getCreator());
                    creator.addRecord(wincoin*creatorPercentage, post.getId(), System.currentTimeMillis());
                    for (String curator : postCurators) {
                        if (curatorEarnings.containsKey(curator)) {
                            curatorEarnings.put(curator, curatorEarnings.get(curator) + (wincoin * (1 - creatorPercentage))/postCurators.size());
                        } else {
                            curatorEarnings.put(curator, (wincoin * (1 - creatorPercentage))/postCurators.size());
                        }
                    }
                }
            }
            for (String curator : curatorEarnings.keySet()) {
                Utente curatorUtente = utenti.get(curator);
                curatorUtente.addRecord(curatorEarnings.get(curator), -1, System.currentTimeMillis());
            }
            lastIteration = System.currentTimeMillis();
            if (multicastAddress != null) {
                try {
                    byte[] message = "update".getBytes();
                    DatagramPacket packet = new DatagramPacket(message, message.length, multicastAddress, multicastPort);
                    System.out.println("Sending update notification");
                    if (socket == null) {
                        socket = new DatagramSocket();
                    }
                    socket.send(packet);
                } catch (SocketException e) {
                    System.out.println("Errore nella creazione del socket");
                } catch (IOException e) {
                    System.out.println("Errore nell'invio del pacchetto di notifica");
                }
            }
        }
    }
}
