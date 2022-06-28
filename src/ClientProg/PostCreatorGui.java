package ClientProg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class PostCreatorGui {

    private JPanel panel;
    public PostCreatorGui(ServerConnection sn) throws IOException {
        JPanel panel = new JPanel(new GridLayout(5, 1));
        panel.setBackground(Color.DARK_GRAY);
        Color color = new Color(0xB0D4B0);

        JPanel titlePanel = new JPanel(new GridLayout(1, 2));
        titlePanel.setBackground(color);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel title = new JLabel("Title");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(Color.DARK_GRAY);
        title.setBackground(color);
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setOpaque(true);
        titlePanel.add(title);
        JTextField titleField = new JTextField();
        titleField.setFont(new Font("Arial", Font.PLAIN, 15));
        titleField.setForeground(Color.DARK_GRAY);
        titleField.setBackground(Color.LIGHT_GRAY);
        titleField.setOpaque(true);
        titleField.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        titlePanel.add(titleField);
        panel.add(titlePanel);

        JPanel contentPanel = new JPanel(new GridLayout(1, 2));
        contentPanel.setBackground(color);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel content = new JLabel("Content");
        content.setFont(new Font("Arial", Font.BOLD, 20));
        content.setForeground(Color.DARK_GRAY);
        content.setHorizontalAlignment(JLabel.CENTER);
        content.setBackground(color);
        content.setOpaque(true);
        contentPanel.add(content);
        JTextArea contentField = new JTextArea(3, 20);
        contentField.setFont(new Font("Arial", Font.PLAIN, 15));
        contentField.setForeground(Color.DARK_GRAY);
        contentField.setBackground(Color.LIGHT_GRAY);
        contentField.setOpaque(true);
        contentField.setLineWrap(true);
        contentField.setWrapStyleWord(true);
        contentField.setEditable(true);
        contentField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        contentPanel.add(contentField);
        panel.add(contentPanel);

        JPanel sendPanel = new JPanel(new GridLayout(1, 1));
        sendPanel.setBackground(color.darker());
        sendPanel.setOpaque(true);
        sendPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JButton send = new JButton("Send");
        send.setFont(new Font("Arial", Font.BOLD, 20));
        send.setForeground(Color.DARK_GRAY);
        send.setBackground(color.brighter());
        send.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String title = titleField.getText();
                String content = contentField.getText();
                if (title.equals("") || content.equals("")) {
                    JOptionPane.showMessageDialog(null, "Please fill in all fields");
                } else {
                    try {
                        sn.post(title, content);
                        titleField.setText("");
                        contentField.setText("");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        sendPanel.add(send);
        panel.add(sendPanel);

        this.panel = panel;
    }

    public JPanel getPanel() {
        return panel;
    }
}
