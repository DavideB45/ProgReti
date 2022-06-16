package ClientProg;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class MainClientGUI {
    public static void main(String[] args) {
        JFrame frame = new JFrame("MainClientGUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        SimplePost[] posts = new SimplePost[3];
        posts[0] = new SimplePost("post1", "username1", "text1 che io sono un bel programmatore, come dice anche il mio attestato di programmazione");
        posts[1] = new SimplePost("post2", "username2", "text2");
        posts[2] = new SimplePost("post3", "username3", "text3");

        JPanel panel = new JPanel(new GridLayout(3, 1));
        for (int i = 0; i < posts.length; i++) {
            JPanel jp = new PostGui(posts[i], new Color(0xB0D4B0)).getPanel();
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
