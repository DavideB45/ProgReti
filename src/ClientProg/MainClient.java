package ClientProg;

import ServerProg.Enrollment;
import ServerProg.SocialNetwork;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;

public class MainClient {
    public static void main(String[] args){
        // connect to local server
        Socket socket;
        ServerProg.Enrollment stub;
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost",8081);
            stub = (ServerProg.Enrollment) registry.lookup("WINSOME");
            System.out.println(Arrays.stream(registry.list()).findFirst().get());
            System.out.println(stub.register("usernameNuovo", "password", new ArrayList<String>()));
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return;
        } catch (IllegalArgumentException e){
            System.out.println("username già in uso\nADDIOS");
            return;
        }
        // register
        try {
            System.out.println(stub.randomMethod());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        // send request
        while (true) {
            try {
                System.in.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                socket = new Socket("localhost", 8080);
                System.out.println("Connected to localhost:8080");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try (OutputStream oStr = socket.getOutputStream();
                 BufferedReader iStr = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                oStr.write("Sono un client\n".getBytes(StandardCharsets.UTF_8), 0, "Sono un client\n".getBytes(StandardCharsets.UTF_8).length);
                String answer = iStr.readLine();
                System.out.println("Ans  : " + answer);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
