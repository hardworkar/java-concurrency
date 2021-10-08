import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args){
        int n = 4;
        Semaphore[] semaphores = new Semaphore[n];
        for(int i = 0 ; i < n ; i++){
            semaphores[i] = new Semaphore(0);
        }
        Worker[] workers = new Worker[n];
        Character[] types = {'A', 'B', 'C', 'W'};
        int[] secs = {1, 2, 3, 0};
        for(int i = 0 ; i < 4 ; i++){
            workers[i] = new Worker(types[i], secs[i], semaphores);
            (new Thread(workers[i])).start();
        }
    }
}
