import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Calculator {
    private final int threads;
    private final List<PartCalculator> workers;
    private final CyclicBarrier barrier;
    private final CyclicBarrier lastBarrier;
    private long maxIteration;

    private void setMaxIteration(){
        maxIteration = workers.stream().map(PartCalculator::getFracIdx).reduce(Long::max).orElse(-1L);
        System.out.println("max iter is: " + maxIteration);
    }

    public Calculator(final int threads, CyclicBarrier lastBarrier){
        this.threads = threads;
        this.workers = new ArrayList<>(threads);
        for(int i = 0 ; i < threads ; i++){
            this.workers.add(i, new PartCalculator(i));
        }
        this.barrier = new CyclicBarrier(threads, this::setMaxIteration);
        this.lastBarrier = lastBarrier;
    }
    public void start(){
        for(PartCalculator worker : workers){
            worker.start();
        }
    }

    private void interruptAll() {
        for(PartCalculator partCalculator : workers){
            partCalculator.interrupt();
        }
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
            /* дожидаемся завершениях всех */
            try {
                lastBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    /* C:\Users\miner\.jdks\openjdk-16.0.1\bin\java.exe -jar D:\nsu\java-concurrency\Task8\out\artifacts\Task8_jar\Task8.jar 3 */
    public static void main(String[] args){
        int threads = Integer.parseInt(args[0]);
        CyclicBarrier finishBarrier = new CyclicBarrier(threads + 1);
        Calculator calculator = new Calculator(threads, finishBarrier);

        SignalHandler handler = sig -> {
            System.out.println("Signal received");
            calculator.interruptAll();
        };
        Signal.handle(new Signal("INT"), handler);

        calculator.start();
        try {
            finishBarrier.await();
            double result = calculator.workers.stream()
                    .map(PartCalculator::getPartSum)
                    .reduce(Double::sum)
                    .orElse(-1.0);
            System.out.println("pi is: " + result * 4);
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }


    }
}