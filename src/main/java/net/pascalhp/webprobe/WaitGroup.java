package net.pascalhp.webprobe;

public class WaitGroup {
    private final Object lock = new Object();
    private int counter = 0;

    public void enter() {
        synchronized (lock) {
            counter++;
        }
    }

    public void exit() {
        synchronized (lock) {
            counter--;
            if (counter == 0) {
                lock.notifyAll();
            }
        }
    }

    public void waitAllExit() throws InterruptedException {
        synchronized (lock) {
            while (counter > 0) {
                lock.wait();
            }
        }
    }

    public int getCounter() {
        synchronized (lock) {
            return counter;
        }
    }
}
