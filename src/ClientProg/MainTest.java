package ClientProg;

import javax.swing.*;

public class MainTest {
    public static void main(String[] args) {
        JFrame frame = new JFrame("MainTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 300);
        frame.setVisible(true);

        JOptionPane.showConfirmDialog(null,"message", "Notification", JOptionPane.YES_NO_OPTION);
    }
}
