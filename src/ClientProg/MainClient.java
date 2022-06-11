package ClientProg;

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
        /*ServerProg.Enrollment stub;
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry("localhost",8081);
            stub = (ServerProg.Enrollment) registry.lookup("WINSOME");
            System.out.println(Arrays.stream(registry.list()).findFirst().get());
            System.out.println(stub.register("username", "password", new ArrayList<String>()));
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
        }*/

        try {
            socket = new Socket("localhost", 8080);
            System.out.println("Connected to localhost:8080");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (OutputStream oStr = socket.getOutputStream();
             BufferedReader iStr = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                if (!useLoop(oStr, iStr)) {
                    return;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("C'è stato un problema");
            return;
        }
    }
    private static boolean useLoop(OutputStream oStr, BufferedReader iStr){
        try {
            System.in.read();
            Thread.sleep(10);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        try {
            System.out.println("scrivo al server");
            oStr.write("Sono un client\n".getBytes(StandardCharsets.UTF_8), 0, "Sono un client\n".getBytes(StandardCharsets.UTF_8).length);
            System.out.println("scritto");
            String answer = iStr.readLine();
            System.out.println("Ans  : " + answer);
            return true;
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
