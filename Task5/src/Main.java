// https://docs.oracle.com/javase/tutorial/essential/concurrency/interrupt.html
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread child = new Thread(() -> {
            while (true) {
                System.out.println("hehe i'm alive");
                if(Thread.interrupted()) {
                    System.out.println("oh no i'm dead");
                    return;
                }
            }
        });
        child.start();
        Thread.sleep(2000);
        child.interrupt();
    }
}