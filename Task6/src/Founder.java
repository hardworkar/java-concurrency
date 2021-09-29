// https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CyclicBarrier.html
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public final class Founder {
    private final List<Runnable> workers;
    private final CyclicBarrier barrier;
    public Founder(final Company company) {
        this.workers = new ArrayList<>(company.getDepartmentsCount());
        for(int i = 0 ; i < company.getDepartmentsCount() ; i++){
            this.workers.add(i, new Worker(company.getFreeDepartment(i)));
        }
        this.barrier = new CyclicBarrier(company.getDepartmentsCount(), company::showCollaborativeResult);
    }
    public void start() {
        for (final Runnable worker : workers) {
            new Thread(worker).start();
        }
    }
    private class Worker implements Runnable {
        private final Department department;
        public Worker(final Department department){
            this.department = department;
        }
        @Override
        public void run() {
            department.performCalculations();
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args){
        Founder founder = new Founder(new Company(5));
        founder.start();
    }
}