import java.util.concurrent.Semaphore;

class SyncPrinter {
    final int threads;
    final Semaphore[] semaphores;
    public SyncPrinter(int threads){
        this.threads = threads;
        this.semaphores = new Semaphore[threads];
        for(int i = 0 ; i < threads ; i++){
            this.semaphores[i] = new Semaphore(0);
        }
        /* с чего-то нужно начинать */
        this.semaphores[0].release();
    }
    public void printStuff(int id, String toPrint){
        while(true){
            try{
                semaphores[id].acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(toPrint);
            /* передаем палочку */
            semaphores[(id + 1) % threads].release();
        }
    }
}