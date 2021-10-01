public class Main {
    public static void main(String[] args){
       int threads = 2;
       Object mutex = new Object();
       SyncPrinter printer = new SyncPrinter(mutex, threads);
       for(int i = 0 ; i < threads ; i++){
           int finalI = i;
           new Thread(() -> {
               try {
                   printer.printStuff(finalI, "This is " + finalI + " line");
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }).start();
       }
    }
}
