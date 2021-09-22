public class Main {
    public static void main(String[] args){
        Threader threader = new Threader();
        threader.start();
        try {
            threader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i = 0 ; i < 100 ; i++)
            System.out.println("aaaaaaaaaaaaaaa");
    }
}