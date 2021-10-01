public class Main {
    public static void main(String args[]){
        int threads = 2;
        SyncPrinter syncPrinter = new SyncPrinter(threads);
        String[] messages = new String[threads];
        messages[0] = "Я базированный Шлёпа гигачад";
        messages[1] = "А я так просто, Бингус";
        for(int i = 0 ; i < threads ; i++){
            int finalI = i;
            new Thread(() -> {
                syncPrinter.printStuff(finalI, messages[finalI]);
            }).start();
        }
    }
}
