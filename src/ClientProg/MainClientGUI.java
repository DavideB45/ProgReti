package ClientProg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

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
        System.out.println("Connessione al server effettuata");
        System.out.print("Inserisci il tuo username : ");
        String username = new BufferedReader(new InputStreamReader(System.in)).readLine();
        System.out.print("Inserisci la tua password : ");
        String password = new BufferedReader(new InputStreamReader(System.in)).readLine();
        //String username = "BillionareGates";
        //String password = "psw";
        String answer = serverConn.login(username, password);
        if(!answer.contains(":")){
            System.out.println("Login effettuato");
            System.out.println("NOTA: la finestra potrebbe essere creata ma non in primo piano");
        }
        else{
            System.out.println("Login fallito");
            return;
        }
        String feed = serverConn.showFeed().trim();
        String[] splitFeed = feed.split("-");
        final ArrayList<String>[] feedList = new ArrayList[]{new ArrayList<>()};
        for (String post : splitFeed) {
            if(post.equals("") || post.equals("\n"))
                continue;
            feedList[0].add(post.trim().split("\t")[0]);
        }
        final int[] currentPage = {0};
        final JPanel[] panel = {createHome(feedList[0], serverConn, currentPage[0])};

        frame.add(panel[0], BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JButton back = new JButton("⇠");
        back.setBackground(Color.DARK_GRAY);
        back.setForeground(Color.GREEN);
        back.setFont(new Font("Arial", Font.BOLD, 20));
        back.setOpaque(true);
        back.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(currentPage[0] > 0){
                    currentPage[0]--;
                    frame.remove(panel[0]);
                    panel[0] = createHome(feedList[0], serverConn, currentPage[0]);
                    frame.add(panel[0], BorderLayout.CENTER);
                    frame.revalidate();
                }
            }
        });
        menuBar.add(back);

        JButton refresh = new JButton("⟳");
        refresh.setBackground(Color.DARK_GRAY);
        refresh.setForeground(Color.GREEN);
        refresh.setFont(new Font("Arial", Font.BOLD, 20));
        refresh.setOpaque(true);
        refresh.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                frame.remove(panel[0]);
                String feed = null;
                try {
                    feed = serverConn.showFeed().trim();
                    String[] splitFeed = feed.split("-");
                    feedList[0] = new ArrayList<>();
                    for (String post : splitFeed) {
                        if(post.equals("") || post.equals("\n"))
                            continue;
                        feedList[0].add(post.trim().split("\t")[0]);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                currentPage[0] = 0;
                panel[0] = createHome(feedList[0], serverConn, currentPage[0]);
                frame.add(panel[0], BorderLayout.CENTER);
                frame.revalidate();
            }
        });
        menuBar.add(refresh);

        JButton forward = new JButton("⇢");
        forward.setBackground(Color.DARK_GRAY);
        forward.setForeground(Color.GREEN);
        forward.setFont(new Font("Arial", Font.BOLD, 20));
        forward.setOpaque(true);
        forward.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                if(currentPage[0] < feedList[0].size()/3){
                    currentPage[0]++;
                    try {
                        String newFeedList = serverConn.showFeed().trim();
                        String[] splitFeed = newFeedList.split("-");
                        for (String post : splitFeed) {
                            if(post.equals("") || post.equals("\n"))
                                continue;
                            if(feedList[0].contains(post.trim().split("\t")[0]))
                                continue;
                            feedList[0].add(post.trim().split("\t")[0]);
                        }
                    } catch (IOException ignore) {
                    }

                    frame.remove(panel[0]);
                    panel[0] = createHome(feedList[0], serverConn, currentPage[0]);
                    frame.add(panel[0], BorderLayout.CENTER);
                    frame.revalidate();
                }
            }
        });
        menuBar.add(forward);

        JButton profile = new JButton("P");
        profile.setBackground(Color.DARK_GRAY);
        profile.setForeground(Color.GREEN);
        profile.setFont(new Font("Arial", Font.BOLD, 20));
        profile.setOpaque(true);
        profile.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                frame.remove(panel[0]);
                back.setEnabled(false);
                forward.setEnabled(false);
                refresh.setEnabled(false);
                panel[0] = createProfile(serverConn);
                frame.add(panel[0], BorderLayout.CENTER);
                frame.revalidate();
            }
        });
        menuBar.add(profile);

        JButton home = new JButton("H");
        home.setBackground(Color.DARK_GRAY);
        home.setForeground(Color.GREEN);
        home.setFont(new Font("Arial", Font.BOLD, 20));
        home.setOpaque(true);
        home.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                frame.remove(panel[0]);
                back.setEnabled(true);
                forward.setEnabled(true);
                refresh.setEnabled(true);
                panel[0] = createHome(feedList[0], serverConn, currentPage[0]);
                frame.add(panel[0], BorderLayout.CENTER);
                frame.revalidate();
            }
        });
        menuBar.add(home);

        JMenu menu = createLastButton(serverConn);

        JPanel postPanel = new JPanel();
        postPanel.setBackground(Color.DARK_GRAY);
        postPanel.setLayout(new GridLayout(1, 1));
        JButton post = new JButton("new post");
        post.setBackground(Color.DARK_GRAY);
        post.setForeground(Color.GREEN);
        post.setFont(new Font("Arial", Font.BOLD, 20));
        post.setOpaque(true);
        post.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    frame.remove(panel[0]);
                    back.setEnabled(false);
                    forward.setEnabled(false);
                    refresh.setEnabled(false);
                    panel[0] = (new PostCreatorGui(serverConn)).getPanel();
                    frame.add(panel[0], BorderLayout.CENTER);
                    frame.revalidate();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        postPanel.add(post);
        menu.add(postPanel);

        menuBar.add(menu);


        frame.add(menuBar, BorderLayout.NORTH);


        frame.setSize(410, 620);
        frame.setBackground(Color.DARK_GRAY);
        frame.setResizable(false);
        frame.toFront();
        frame.requestFocus();
        frame.setVisible(true);
    }

    private static JPanel createHome(ArrayList<String> feedList, ServerConnection serverConn, int page){
        JPanel panel = new JPanel(new GridLayout(3, 1));
        int missing = 3;
        for (int i = page*3; i < page*3 + 3 && i < feedList.size(); i++) {
            JPanel jp = null;
            try {
                jp = new PostGui(feedList.get(i), new Color(0xB0D4B0), serverConn).getPanel();
                jp.setBackground(Color.DARK_GRAY);
                panel.add(jp);
                missing--;
            } catch (IOException e) {
                e.printStackTrace();
                missing++;
            }
        }
        for (int i = 0; i < missing; i++) {
            JPanel jp = new JPanel();
            jp.setBackground(Color.DARK_GRAY);
            panel.add(jp);
        }
        return panel;
    }

    private static JPanel createProfile(ServerConnection sn){
        JPanel panel;
        try {
            ArrayList<PostHead> posts = sn.viewBlogObj();
            panel = new JPanel(new GridLayout(20, 1));
            for (PostHead post : posts) {
                JPanel jp = new JPanel(new GridLayout(1, 2));
                jp.setBackground(Color.DARK_GRAY);
                JLabel label = new JLabel("  " + post.getTitle());
                label.setFont(new Font("Arial", Font.PLAIN, 20));
                label.setForeground(Color.WHITE);
                jp.add(label);
                JButton del = new JButton("delete");
                del.setBackground(Color.DARK_GRAY);
                del.setForeground(Color.RED);
                del.setFont(new Font("Arial", Font.BOLD, 20));
                del.setOpaque(true);
                del.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        try {
                            String effect = sn.deletePost(String.valueOf(post.getId()));
                            if(effect.contains("post")){
                                del.setText("deleted");
                                del.setBackground(Color.RED);
                                del.setEnabled(false);
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                JButton show = new JButton("show");
                show.setBackground(Color.DARK_GRAY);
                show.setForeground(Color.GREEN);
                show.setFont(new Font("Arial", Font.BOLD, 20));
                show.setOpaque(true);
                show.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        try {
                            new PersonalPostGui(String.valueOf(post.getId()), new Color(0xB0D4B0), sn);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                GridLayout buttons = new GridLayout(1, 2);
                buttons.setHgap(10);
                JPanel buttonsPanel = new JPanel(buttons);
                buttonsPanel.setBackground(Color.DARK_GRAY);
                buttonsPanel.add(del);
                buttonsPanel.add(show);
                jp.add(buttonsPanel);
                //jp.add(del);
                panel.add(jp);
                JLabel space = new JLabel(" ");
                space.setBackground(Color.DARK_GRAY);
                space.setOpaque(true);
                panel.add(space);
            }
        } catch (IOException | NullPointerException e) {
            panel = new JPanel();
            panel.setBackground(Color.DARK_GRAY);
            panel.add(new JLabel("No posts"));
        }
        panel.setBackground(Color.DARK_GRAY);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setOpaque(true);

        return panel;
    }

    private static JMenu createLastButton(ServerConnection serverConn){
        JMenu menu = new JMenu(" = ");
        menu.setBackground(Color.DARK_GRAY);
        menu.setForeground(Color.DARK_GRAY);
        menu.setFont(new Font("Arial", Font.BOLD, 20));
        menu.setOpaque(true);
        menu.add(startFollow(serverConn));
        menu.add(stopFollow(serverConn));
        return menu;
    }

    private static JPanel startFollow(ServerConnection serverConn){
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.setBackground(Color.DARK_GRAY);
        panel.setOpaque(true);
        JTextArea toFollow = new JTextArea();
        toFollow.setBackground(Color.LIGHT_GRAY);
        toFollow.setForeground(Color.DARK_GRAY);
        toFollow.setOpaque(true);
        toFollow.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(toFollow);
        JButton follow = new JButton("follow");
        follow.setBackground(Color.DARK_GRAY);
        follow.setForeground(Color.GREEN);
        follow.setFont(new Font("Arial", Font.BOLD, 20));
        follow.setOpaque(true);
        follow.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    if(toFollow.getText().length() < 3) {
                        return;
                    }
                    String effect = serverConn.follow(toFollow.getText());
                    if(effect.contains("following")){
                        toFollow.setText("followed");
                    } else {
                        toFollow.setText(" ");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        panel.add(follow);
        return panel;
    }

    private static JPanel stopFollow(ServerConnection serverConn){
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.setBackground(Color.DARK_GRAY);
        panel.setOpaque(true);
        JTextArea toUnfollow = new JTextArea();
        toUnfollow.setBackground(Color.LIGHT_GRAY);
        toUnfollow.setForeground(Color.DARK_GRAY);
        toUnfollow.setOpaque(true);
        toUnfollow.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(toUnfollow);
        JButton unf = new JButton("unfollow");
        unf.setBackground(Color.DARK_GRAY);
        unf.setForeground(Color.RED);
        unf.setFont(new Font("Arial", Font.BOLD, 20));
        unf.setOpaque(true);
        unf.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    if(toUnfollow.getText().length() < 3) {
                        return;
                    }
                    String effect = serverConn.unfollow(toUnfollow.getText());
                    if(effect.contains("unfollowing")){
                        toUnfollow.setText("unfollowed");
                    } else {
                        toUnfollow.setText(" ");
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        panel.add(unf);
        return panel;
    }


}
