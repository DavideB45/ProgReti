package ClientProg;

import ServerProg.Comment;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class PersonalPostGui {
    private JPanel panel;

    public PersonalPostGui(String id, Color color, ServerConnection sn) throws IOException {
        JFrame frame = new JFrame("PersonalPostGui");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        SimplePost post = sn.showPostObj(id);
        JPanel panel = new JPanel(new GridLayout(3 + post.getComments().size(), 1));
        panel.setBackground(color.darker());

        JLabel title = new JLabel(post.getUsername() + " - " + post.getTitle());
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.DARK_GRAY);
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setBackground(color);
        title.setOpaque(true);
        panel.add(title);


        JTextArea content = new JTextArea(post.getContent());
        content.setFont(new Font("Arial", Font.PLAIN, 15));
        content.setForeground(Color.DARK_GRAY);
        content.setBackground(color);
        content.setLineWrap(true);
        content.setWrapStyleWord(true);
        content.setEditable(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        panel.add(content);


        JLabel rating = new JLabel(" ↑ " + post.getLikes() + " ↓ " + post.getDislikes());
        rating.setFont(new Font("Arial", Font.PLAIN, 20));
        rating.setForeground(Color.DARK_GRAY);
        rating.setHorizontalAlignment(JLabel.CENTER);
        rating.setBackground(color);
        rating.setOpaque(true);
        panel.add(rating);

        for (Comment value : post.getComments()) {
            JLabel comm = new JLabel(value.getUsername() + ": " + value.getText());
            comm.setFont(new Font("Arial", Font.PLAIN, 18));
            comm.setForeground(Color.BLACK);
            comm.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            panel.add(comm);
        }

        frame.add(panel);
        frame.setSize(400, post.getComments().size() * 50 + 250);
        frame.paint(frame.getGraphics());
        frame.setVisible(true);
        frame.invalidate();
    }
}
