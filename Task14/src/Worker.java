import java.util.concurrent.Semaphore;

class Worker implements Runnable{
    private final Semaphore A, B, C;
    private final Character partName;
    private final int secs;
    public Worker(Character partName, int secs, Semaphore[] semaphores){
        this.partName = partName;
        this.secs = secs;
        this.A = semaphores[0];
        this.B = semaphores[1];
        this.C = semaphores[2];
    }
    private void work(){
        try {
            Thread.sleep(secs * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void run() {
        while (true) {
            switch (partName) {
                case 'A' -> {
                    work();
                    A.release();
                    System.out.println("New A! : " + System.nanoTime() / 1e9);
                }
                case 'B' -> {
                    work();
                    B.release();
                    System.out.println("New B! : " + System.nanoTime() / 1e9);
                }
                case 'C' -> {
                    work();
                    C.release();
                    System.out.println("New C! : " + System.nanoTime() / 1e9);
                }
                case 'W' -> {
                    try {
                        A.acquire();
                        B.acquire();
                        C.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("New Widget! : " +
                            "[" + A.availablePermits() + " ; " + B.availablePermits() + " ; " + C.availablePermits() + "] "
                            + System.nanoTime() / 1e9);
                }
            }
        }
    }
}
