package ClientProg;

import ServerProg.Post;

import javax.swing.*;
import java.awt.*;

public class PostGui {
    private JPanel panel;

    public PostGui(SimplePost post, Color color) {
        JPanel panel1 = new JPanel(new GridLayout(3, 1));
            JLabel textArea = new JLabel(post.getUsername() + " - " + post.getTitle());
            textArea.setFont(new Font("Arial", Font.BOLD, 16));
            textArea.setHorizontalAlignment(JLabel.CENTER);
            textArea.setVerticalAlignment(JLabel.CENTER);
            textArea.setForeground(Color.DARK_GRAY);
            textArea.setOpaque(true);
            textArea.setBackground(color);
            panel1.add(textArea);
            JTextArea label = new JTextArea(post.getContent());
            label.setLineWrap(true);
            label.setWrapStyleWord(true);
            label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            label.setMinimumSize(new Dimension(50, 20));
            label.setBackground(color.darker());
            panel1.add(label);
            JPanel panel2 = new JPanel(new GridLayout(1, 3));
                panel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                panel2.setBackground(color.darker());
                panel2.add(new JButton("like"));
                panel2.add(new JButton("comment"));
                panel2.add(new JButton("dislike"));
            panel1.add(panel2);

        panel = panel1;
    }

    JPanel getPanel() {
        return panel;
    }
}
