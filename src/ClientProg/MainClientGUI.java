package ClientProg;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.util.ArrayList;

public class MainClientGUI {
    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("MainClientGUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        ServerConnection serverConn;
        try {
            if(args.length < 1){
                System.out.println("missing config file");
                return;
            }
            serverConn = new ServerConnection(args[0]);
        } catch (IOException | NotBoundException e) {
            System.out.println("Impossibile connettersi al server");
            return;
        }
        /*System.out.println("Connessione al server effettuata");
        System.out.print("Inserisci il tuo username : ");
        String username = new BufferedReader(new InputStreamReader(System.in)).readLine();
        System.out.print("Inserisci la tua password : ");
        String password = new BufferedReader(new InputStreamReader(System.in)).readLine();*/
        String username = "BillionareGates";
        String password = "psw";
        String answer = serverConn.login(username, password);
        if(!answer.contains(":")){
            System.out.println("Login effettuato");
        }
        else{
            System.out.println("Login fallito");
            return;
        }
        String feed = serverConn.showFeed().trim();
        String[] splitFeed = feed.split("-");
        ArrayList<String> feedList = new ArrayList<>();
        for (String post : splitFeed) {
            if(post.equals("") || post.equals("\n"))
                continue;
            feedList.add(post.trim().split("\t")[0]);
        }
        JPanel panel = new JPanel(new GridLayout(3, 1));
        for (int i = 0; i < 3 && i < feedList.size(); i++) {
            JPanel jp = new PostGui(feedList.get(i), new Color(0xB0D4B0), serverConn).getPanel();
            panel.add(jp);
        }
        for (int i = feedList.size(); i < 3; i++) {
            JPanel jp = new JPanel();
            panel.add(jp);
        }

        frame.add(panel, BorderLayout.CENTER);
        panel.getComponent(0).setBackground(Color.DARK_GRAY);
        panel.getComponent(1).setBackground(Color.DARK_GRAY);
        panel.getComponent(2).setBackground(Color.DARK_GRAY);

        frame.setSize(400, 600);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
