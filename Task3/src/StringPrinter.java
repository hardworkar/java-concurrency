
public class StringPrinter extends Thread{
    private final String[] stringsToPrint;
    public StringPrinter(String[] stringToPrint){
        this.stringsToPrint = stringToPrint;
    }
    private synchronized void sameMethod(String[] strings){
        for(var s : strings){
            System.out.println(s);
        }
    }
    public void run(){
        sameMethod(stringsToPrint);
    }
}
