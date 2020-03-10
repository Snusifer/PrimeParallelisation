import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;

public class ParallelPrimeFactorisation extends SequentialPrimeFactorisation {
    private final int nThreads;
    private final ExecutorService ex;

    public ParallelPrimeFactorisation(int[] primes, int nThreads) {
        super(primes);
        this.nThreads = nThreads <= 0 ? Runtime.getRuntime().availableProcessors() : nThreads;
        ex = Executors.newFixedThreadPool(this.nThreads);
    }

    public void shutdown() {
        ex.shutdown();
    }

    @Override
    public List<Long> factorise(long n) {
        List<Long> solution = new ArrayList<>();
        FactorMonitor monitor = new FactorMonitor(n);

        List<Task> tasks = new ArrayList<>();

        for (int i = 0; i < nThreads; i ++) {
            tasks.add(new Task(i, monitor));
        }

        try {
            List<Future<List<Long>>> futures = ex.invokeAll(tasks);
            for (Future<List<Long>> future : futures) {
                solution.addAll(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (monitor.getN() != 1) {
            solution.add(monitor.getN());
        }

        return solution;
    }

    private class Task implements Callable<List<Long>> {
        int id;
        FactorMonitor monitor;
        List<Long> localFactors = new ArrayList<>();

        public Task(int id, FactorMonitor monitor) {
            this.id = id;
            this.monitor = monitor;
        }

        @Override
        public List<Long> call() throws Exception {
            int p;
            for (int i = id; i < primes.length; i += nThreads) {
                p = primes[i];
                if (Math.pow(p, 2) > monitor.getN())
                    break;

                while (monitor.getN() % p == 0) {
                    monitor.divideNumberByPrime(p);
                    localFactors.add((long) p);
                }
            }
            return localFactors;
        }
    }

    private static boolean testFactorisation(long n, long k, int nThreads) {
        ParallelSieve p = new ParallelSieve((int) Math.sqrt(n) + 1, 512);
        int[] primes = p.initParallel();
        p.shutdown();
        System.out.println("Siebe");

        SequentialPrimeFactorisation seq = new SequentialPrimeFactorisation(primes);
        ParallelPrimeFactorisation par = new ParallelPrimeFactorisation(primes, nThreads);
        for (long i = n - k; i < n; i++) {
            if (i % 100 == 0) {
                System.out.println(i + " now.");
            }
            List<Long> seqFactors = seq.factorise(i);
            List<Long> parFactors = par.factorise(i);

            Collections.sort(seqFactors);
            Collections.sort(parFactors);

            if (seqFactors.size() != parFactors.size()) {
                par.shutdown();
                return false;
            }
            for (int j = 0; j < seqFactors.size(); j++) {
                if (!seqFactors.get(j).equals(parFactors.get(j))) {
                    par.shutdown();
                    return false;
                }
            }
        }
        par.shutdown();
        return true;
    }

    public static void main(String[] args) {
        if (!(ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-ea"))) {
            System.out.println("IMPORTANT: Include -ea as flag to run the test");
            return;
        }

        if(!(args.length == 3)) {
            System.out.print("Invalid number of arguments!\nExpected 3, got " + (args.length));
            return;
        }
        long n;
        long k;
        int nThreads;
        try {
            n = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Argument 1 must be of long type!");
            return;
        }
        try {
            k = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Argument 2 must be of long type!");
            return;
        }
        try {
            nThreads = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Argument 3 must be integer!");
            return;
        }
        assert testFactorisation(n, k, nThreads);
        System.out.println("Test complete. No errors.");
    }
}
