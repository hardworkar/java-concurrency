import java.util.concurrent.locks.ReentrantLock;

public class Philosopher implements Runnable{
    private final ReentrantLock leftFork, rightFork;
    private final Object forks;
    private Boolean flag;
    public Philosopher(ReentrantLock leftFork, ReentrantLock rightFork, final Object forks, final Boolean flag){
        this.leftFork = leftFork;
        this.rightFork = rightFork;
        this.forks = forks;
        this.flag = flag;

    }
    private void log(String action){
        System.out.println(Thread.currentThread().getName() + " in " + System.nanoTime() + " " + action);
    }
    private void act(String action) {
        log(action);
        try {
            Thread.sleep((long) (Math.random() * 100));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        while(true){

            act("thinking");

            synchronized (forks){
                while(true) {
                    if (leftFork.tryLock()) {
                        log("picked left: " + leftFork);
                        if (rightFork.tryLock()) {
                            log("picked right: " + rightFork);
                            flag = true;
                            forks.notifyAll();
                            break;
                        } else {
                            leftFork.unlock();
                            log("realised left: " + leftFork);
                            while(!flag) {
                                try {
                                    forks.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            flag = false;
                        }
                    } else {
                        while(!flag) {
                            try {
                                forks.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        flag = false;
                    }
                }
            }

            act("eating");

            leftFork.unlock();
            log("realised left: " + leftFork);
            rightFork.unlock();
            log("realised right: " + rightFork);

        }
    }
    public static void main(String[] args){
        Philosopher[] philosophers = new Philosopher[5];
        ReentrantLock[] forks = new ReentrantLock[5];
        Boolean flag = Boolean.FALSE;
        Object forks_m = new Object();
        for(int i = 0 ; i < 5 ; i++){
            forks[i] = new ReentrantLock(true);
        }
        for(int i = 0 ; i < 5 ; i++){
            philosophers[i] = new Philosopher(forks[i], forks[(i + 1) % 5], forks_m, flag);
            (new Thread(philosophers[i], "Philosopher " + (i + 1))).start();
        }
    }
}