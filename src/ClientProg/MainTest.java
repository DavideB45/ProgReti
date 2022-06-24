package ClientProg;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

public class MainTest {
    public static void main(String[] args) throws IOException {
        ServerConnection serverConn;
        Enumeration<NetworkInterface> networkInterface = NetworkInterface.getNetworkInterfaces();
        // print all available network interfaces
        while (networkInterface.hasMoreElements()) {
            NetworkInterface iface = networkInterface.nextElement();
            System.out.println(iface.getName());
            System.out.println(iface.supportsMulticast());
            System.out.println(iface.isLoopback());
            System.out.println("------------------------------------------------------------");
        }
        try {
            serverConn = new ServerConnection(InetAddress.getLocalHost(), 3031);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NotBoundException e) {
            e.printStackTrace();
            return;
        }
        ArrayList<String> tags = new ArrayList<>();
        tags.add("java");
        tags.add("rmi");
        tags.add("concurrent");
        serverConn.register("username", "password", tags);
        serverConn.login("username", "password");
        serverConn.post("TITOLO", "CONTENUTO");
        serverConn.logout();

        tags.clear();
        tags.add("Pesticidi");
        tags.add("Fiori");
        tags.add("Frutti");
        tags.add("Verdura");
        tags.add("Carne");
        tags.add("Pesce");
        System.out.println(serverConn.register("Luisa", "psw", tags));
        serverConn.login("Luisa", "psw");
        serverConn.post("FORMICHE", "creature magnifiche le formiche");
        serverConn.follow("username");
        serverConn.logout();

        serverConn.login("username", "password");
        System.out.println(serverConn.follow("Luisa"));
        System.out.println(serverConn.rate("2", "5"));
        serverConn.comment("2", "melglio non infastidirle");
        System.out.println(serverConn.showPost("2"));
        serverConn.logout();

        tags.clear();
        tags.add("bicycle");
        tags.add("bike");
        tags.add("mountain");
        tags.add("Carrefour");
        serverConn.register("Fabio", "psw", tags);
        serverConn.login("Fabio", "psw");
        serverConn.follow("Luisa");
        serverConn.comment("2", "ben detto");
        serverConn.rate("2", "5");
        System.out.println(serverConn.listFollowing());


    }
}
