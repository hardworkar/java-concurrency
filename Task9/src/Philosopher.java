public class Philosopher implements Runnable{
    private final String leftFork, rightFork;
    public Philosopher(String leftFork, String rightFork){
        this.leftFork = leftFork;
        this.rightFork = rightFork;
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
        /* deadlock example: everyone picks "identical" fork */
        while(true){
            act("thinking");
            synchronized (leftFork){
                log("picked " + leftFork);
                synchronized (rightFork){
                    log("picked " + rightFork);
                    act("eating");
                    log("realised " + rightFork);
                }
                log("realised " + leftFork);
            }
        }
    }
    public static void main(String[] args){
        Philosopher[] philosophers = new Philosopher[5];
        String[] forks = new String[5];
        for(int i = 0 ; i < 5 ; i++){
            forks[i] = "fork " + (i + 1);
        }
        for(int i = 0 ; i < 5 ; i++){
            /* solution (Dijkstra): the first fork to be picked is the lower-numbered one */
            /* this condition already holds for philosophers 1 - 4, we need a slight change for 5th */
            if(i < 4)
                philosophers[i] = new Philosopher(forks[i], forks[(i + 1) % 5]);
            else
                philosophers[i] = new Philosopher(forks[(i + 1) % 5], forks[i]);
            (new Thread(philosophers[i], "Philosopher " + (i + 1))).start();
        }
    }
}
