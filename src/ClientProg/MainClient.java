package ClientProg;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainClient {
    public static void main(String[] args){
        // connect to local server
        Socket socket = null;
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
