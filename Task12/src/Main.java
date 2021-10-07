import java.util.LinkedList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();
        (new Thread(() -> {
            while(true){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (list){
                    bubble_sort(list);
                }
            }
        })).start();

        while(true){
            Scanner in = new Scanner(System.in);
            String s = in.nextLine();
            if(!s.isEmpty()){
                synchronized (list) {
                    list.addFirst(s);
                }
            }
            else{
                synchronized (list) {
                    System.out.println(list);
                }
            }
        }
    }

    private static void bubble_sort(LinkedList<String> list) {
        int n = list.size();
        for (int i = 0; i < n-1; i++)
            for (int j = 0; j < n-i-1; j++)
                if (list.get(j).compareTo(list.get(j+1)) > 0)
                {
                    String tmp = list.get(j);
                    list.set(j, list.get(j+1));
                    list.set(j+1, tmp);
                }
    }
}
