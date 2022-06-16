package ClientProg;

import javax.swing.*;
import java.io.IOException;
import java.net.*;

public class NotificationReceiver implements Runnable{
    MulticastSocket socket;
    private static final int MAX_MESSAGE_DIM = 128;

    public NotificationReceiver(InetAddress multicastAddress, int multicastPort) throws IOException {
        socket = new MulticastSocket(multicastPort);
        socket.joinGroup(multicastAddress);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] buffer = new byte[MAX_MESSAGE_DIM];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                JOptionPane.showConfirmDialog(null,message, "Show wallet?", JOptionPane.YES_NO_OPTION);
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    socket.close();
                    return;
                }
            }
        }
    }
}
