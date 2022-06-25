package ClientProg;

import java.io.IOException;
import java.net.*;

public class NotificationReceiver implements Runnable{
    MulticastSocket socket;
    private static final int MAX_MESSAGE_DIM = 128;

    public NotificationReceiver(InetAddress multicastAddress, int multicastPort) throws IOException {
        socket = new MulticastSocket(multicastPort);
        //NetworkInterface networkInterface = NetworkInterface.getByName("en0");
        //socket.joinGroup(new InetSocketAddress(multicastAddress, multicastPort), networkInterface);
        socket.joinGroup(multicastAddress);
    }

    /**
     * wait on multicast socket
     * print received data
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] buffer = new byte[MAX_MESSAGE_DIM];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                if(Thread.currentThread().isInterrupted())
                    break;
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }
}
