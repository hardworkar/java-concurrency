public class Main {
    public static void main(String[] args) {
        String[][] stringArrays = {{"ok", "boomer"}, {"who's", "your", "president?"}, {"ru", "mine?"}, {"based"}};
        for(int i = 0; i < 4 ; i++){
            StringPrinter stringPrinter = new StringPrinter(stringArrays[i]);
            stringPrinter.start();
        }
    }
}
