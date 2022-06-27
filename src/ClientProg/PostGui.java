package ClientProg;

import ServerProg.Comment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

public class PostGui {
    private final JPanel panel;
    private SimplePost post;
    JButton like;
    JButton dislike;
    JButton comment;

    public PostGui(String id, Color color, ServerConnection serverConn) throws IOException {

        post = serverConn.showPostObj(id);
        String title = post.getTitle();
        String username = post.getUsername();
        String text = post.getContent();
        like = new JButton("Like " + post.getLikes());
        dislike = new JButton("Dislike " + post.getDislikes());
        like.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    serverConn.rate(id, "1");
                    SimplePost reloadedPost = serverConn.showPostObj(id);
                    if (reloadedPost != null) {
                        post = reloadedPost;
                        like.setText("Like " + post.getLikes());
                        dislike.setText("Dislike " + post.getDislikes());
                    }
                    like.setBackground(Color.GREEN);
                    like.setOpaque(true);
                    dislike.setEnabled(false);
                } catch (IOException ignore) {
                }
            }
        });
        dislike.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                try {
                    serverConn.rate(id, "-1");
                    SimplePost reloadedPost = serverConn.showPostObj(id);
                    if (reloadedPost != null) {
                        post = reloadedPost;
                        like.setText("Like " + post.getLikes());
                        dislike.setText("Dislike " + post.getDislikes());
                    }
                    dislike.setText("Dislike " + (post.getDislikes()));
                    dislike.setBackground(Color.RED);
                    dislike.setOpaque(true);
                    like.setEnabled(false);
                } catch (IOException ignore) {
                }
            }
        });
        comment = new JButton("Comment");
        comment.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFrame frame = new JFrame("Comment");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                JPanel panel = new JPanel(new GridLayout(post.getComments().size() + 2, 1));
                ArrayList<Comment> comments = post.getComments();
                for (Comment value : comments) {
                    JPanel jp = new JPanel();
                    jp.setBackground(color.darker());
                    JTextArea comm = new JTextArea(value.getUsername() + ": " + value.getText());
                    comm.setEditable(false);
                    comm.setForeground(Color.WHITE);
                    comm.setBackground(color.darker());
                    jp.add(comm);
                    panel.add(jp);
                }
                JPanel jp2 = new JPanel();
                jp2.setBackground(Color.DARK_GRAY);
                JTextArea textArea = new JTextArea(4, 20);
                textArea.setAutoscrolls(true);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBackground(Color.GRAY);
                jp2.add(textArea);
                panel.add(jp2);
                JPanel jp3 = new JPanel();
                jp3.setBackground(Color.DARK_GRAY);
                JButton send = new JButton("Send");
                send.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            String comment = textArea.getText();
                            serverConn.comment(id, comment);
                            SimplePost reloadedPost = serverConn.showPostObj(id);
                            if (reloadedPost != null) {
                                post = reloadedPost;
                            }
                            JPanel panel = new JPanel(new GridLayout(post.getComments().size() + 2, 1));
                            ArrayList<Comment> comments = post.getComments();
                            for (Comment value : comments) {
                                JPanel jp = new JPanel();
                                jp.setBackground(color.darker());
                                JTextArea comm = new JTextArea(value.getUsername() + ": " + value.getText());
                                comm.setEditable(false);
                                comm.setForeground(Color.WHITE);
                                comm.setBackground(color.darker());
                                jp.add(comm);
                                panel.add(jp);
                            }
                            panel.add(jp2);
                            panel.add(jp3);
                            frame.getContentPane().removeAll();
                            frame.getContentPane().add(panel, BorderLayout.CENTER);
                            frame.setSize(400, post.getComments().size() * 30 + 100);
                            textArea.setText("");
                            frame.revalidate();
                        } catch (IOException ignore) {
                        }
                    }
                });
                jp3.add(send);
                panel.add(jp3);
                frame.add(panel, BorderLayout.CENTER);
                frame.setSize(400, post.getComments().size() * 30 + 100);
                frame.setResizable(false);
                frame.setBackground(Color.DARK_GRAY);
                frame.setVisible(true);
            }
        });
        JPanel panel1 = new JPanel(new GridLayout(3, 1));
            JLabel textArea = new JLabel(username + " - " + title);
            textArea.setFont(new Font("Arial", Font.BOLD, 16));
            textArea.setHorizontalAlignment(JLabel.CENTER);
            textArea.setVerticalAlignment(JLabel.CENTER);
            textArea.setForeground(Color.DARK_GRAY);
            textArea.setOpaque(true);
            textArea.setBackground(color);
            panel1.add(textArea);
            JTextArea label = new JTextArea(text);
            label.setEditable(false);
            label.setLineWrap(true);
            label.setWrapStyleWord(true);
            label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            label.setMinimumSize(new Dimension(50, 20));
            label.setBackground(color.darker());
            panel1.add(label);
            JPanel panel2 = new JPanel(new GridLayout(1, 3));
                panel2.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                panel2.setBackground(color.darker());
                panel2.add(like);
                panel2.add(comment);
                panel2.add(dislike);
            panel1.add(panel2);
        panel = panel1;
    }

    JPanel getPanel() {
        return panel;
    }
}
