package ServerProg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;

public class SysInReader implements Runnable {
    private final AtomicBoolean running;
    private final Selector selector;

    public SysInReader(AtomicBoolean running, Selector selector) {
        this.running = running;
        this.selector = selector;
    }

    @Override
    public void run() {
        String input;
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        while (running.get()) {
            try {
                input = stdIn.readLine();
                if (input.equals("exit")) {
                    System.out.println("Exiting...");
                    running.set(false);
                    selector.wakeup();
                }
                System.out.println("Input: " + input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
