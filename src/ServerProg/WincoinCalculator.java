package ServerProg;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class WincoinCalculator implements Runnable {
    private final long iterationsInterleave;
    private final ConcurrentHashMap<Integer, Post> posts;
    private final ConcurrentHashMap<String, Utente> utenti;
    private InetAddress multicastAddress;
    private final int multicastPort;

    public WincoinCalculator(long iterationsInterleave, ConcurrentHashMap<Integer, Post> posts, ConcurrentHashMap<String, Utente> utenti, String multicastAddress, int multicastPort) {
        this.iterationsInterleave = iterationsInterleave;
        this.posts = posts;
        this.utenti = utenti;
        this.multicastPort = multicastPort;
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
            for (Post post : posts.values()) {
                float wincoin = post.calculateWincoin();
                if (wincoin > 0) {
                    Utente creator = utenti.get(post.getCreator());
                    creator.addRecord(wincoin, post.getId(), System.currentTimeMillis());
                }
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
