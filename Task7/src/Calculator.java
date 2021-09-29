import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Calculator {
    private final int threads;
    private final int iterations;
    private final List<PartCalculator> workers;
    private final CyclicBarrier barrier;
    public Calculator(final int threads, final int iterations){
        this.threads = threads;
        this.iterations = iterations;
        this.workers = new ArrayList<>(threads);
        for(int i = 0 ; i < threads ; i++){
            this.workers.add(i, new PartCalculator(i));
        }
        this.barrier = new CyclicBarrier(threads, this::printResult);
    }
    public void start(){
        for(PartCalculator worker : workers){
            new Thread(worker).start();
        }
    }
    private void printResult(){
        System.out.println("Computations finished.");
        final double result = workers.stream()
                .map(PartCalculator::getPartSum)
                .reduce(Double::sum)
                .orElse(-1.0);
        System.out.println("π is: " + result * 4);
    }
    class PartCalculator implements Runnable{
        private final int idx;
        private double partSum = 0.0;
        public PartCalculator(final int idx){
            this.idx = idx;
        }
        @Override
        public void run() {
            double fracIdx = idx;
            for(; fracIdx < iterations ; fracIdx += threads){
                double fraction;
                if(fracIdx % 2 == 0)
                    fraction = 1;
                else
                    fraction = -1;
                fraction /= 2 * fracIdx + 1;
                partSum += fraction;
            }
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
        public synchronized double getPartSum() {
            return partSum;
        }
    }
    public static void main(String[] args){
        int threads = Integer.parseInt(args[0]);
        int iterations = 200000000;
        Calculator calculator = new Calculator(threads, iterations);
        calculator.start();
    }
}
