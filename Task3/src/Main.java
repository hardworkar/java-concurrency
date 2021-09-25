public class Main {
    public static void main(String[] args) throws InterruptedException {
        String[][] stringArrays = {{"ok", "boomer", "базированный Шлепа", "軟盤", "оооооооочень длинная строоооокаааааа"}, {"who's", "your", "president?"}, {"ru", "mine?"}, {"based"}};
        Thread toWait = null;
        for(int i = 0; i < 4 ; i++){
            StringPrinter stringPrinter = new StringPrinter(stringArrays[i]);
            if(toWait != null) {
                toWait.join();
            }
            stringPrinter.start();
            toWait = stringPrinter;
        }
    }
}
