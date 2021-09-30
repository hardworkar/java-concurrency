import sun.misc.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Calculator {
    private final int threads;
    private final List<PartCalculator> workers;
    private final CyclicBarrier barrier;
    private long maxIteration;

    private void setMaxIteration(){
        maxIteration = workers.stream().map(PartCalculator::getFracIdx).reduce(Long::max).orElse(-1L);
        System.out.println("max iter is: " + maxIteration);
    }

    public Calculator(final int threads){
        this.threads = threads;
        this.workers = new ArrayList<>(threads);
        for(int i = 0 ; i < threads ; i++){
            this.workers.add(i, new PartCalculator(i));
        }
        this.barrier = new CyclicBarrier(threads, this::setMaxIteration);
    }
    public void start(){
        for(PartCalculator worker : workers){
            worker.start();
        }
    }

    private void printResult(){
        for(PartCalculator partCalculator : workers){
            partCalculator.interrupt();
        }

        for(PartCalculator partCalculator : workers) {
            try {
                partCalculator.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        double result = workers.stream()
            .map(PartCalculator::getPartSum)
            .reduce(Double::sum)
            .orElse(-1.0);
        System.out.println("π is: " + result * 4);
    }
    class PartCalculator extends Thread{
        private double partSum = 0.0;
        private long fracIdx;

        public PartCalculator(final int idx){
            this.fracIdx = idx;
        }
        public synchronized long getFracIdx() {
            return fracIdx;
        }
        public synchronized double getPartSum() {
            return partSum;
        }

        public void run() {
            while(!interrupted()){
                partSum += (fracIdx % 2 == 0 ? 1.0 : -1.0) / (2.0 * fracIdx + 1.0);
                fracIdx += threads;
            }

            /* нас прервали */
            /* ждем пока все поймут что пора заканчивать */

            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }

            /* к этому моменту нам положили максимум в переменную */
            /* подсчитываем до максимума, завершаемся */
            for(; fracIdx <= maxIteration; fracIdx += threads){
                partSum += (fracIdx % 2 == 0 ? 1.0 : -1.0) / (2.0 * fracIdx + 1.0);
            }
        }
    }
    public static void main(String[] args){
        int threads = Integer.parseInt(args[0]);
        Calculator calculator = new Calculator(threads);

        calculator.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Signal received");
            calculator.printResult();
        }));

        /*
        Signal.handle(new Signal("INT"), signal -> {
            System.out.println("Signal received");
            calculator.printResult();
        });
        */

        new Thread(()->{
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Signal.raise(new Signal("INT"));
        }).start();
    }
}