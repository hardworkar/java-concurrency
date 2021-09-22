public class Main {
    public static void main(String[] args){
        Threader threader = new Threader();
        threader.start();
        for(int i = 0 ; i < 100 ; i++)
            System.out.println("aaaaaaaaaaaaaaa");
    }
}
