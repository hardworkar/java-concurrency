class SyncPrinter {
    private int turn = 0;
    private final int threads;
    private final Object mutex;
    public SyncPrinter(Object mutex, int threads){
        this.threads = threads;
        this.mutex = mutex;
    }
    public void printStuff(int id, String toPrint) throws InterruptedException {
        while(true){
            synchronized (mutex) {
                while(turn != id)
                    mutex.wait();
                System.out.println(toPrint);
                turn = (turn + 1) % threads;
                mutex.notifyAll();
                /* спинлоки, рекурсивлоки, интеррапты */
            }
        }
    }
}
