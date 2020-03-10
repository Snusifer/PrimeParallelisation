import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ParallelSieve extends SequentialSieve {
    private final int nThreads;
    private final ExecutorService ex;
    private final List<int[]> primes = new ArrayList<>();

    public ParallelSieve(int n, int nThreads) {
        super(n);
        this.nThreads = nThreads <= 0 ? Runtime.getRuntime().availableProcessors() : nThreads;
        this.ex = Executors.newFixedThreadPool(this.nThreads);
        primes.add(new int[] {3});
    }

    private int[] convertToArray() {
        int[] primesArray = new int[getPrimesCount()];
        int i = 0;
        for (int[] pList : primes)
            for (int p : pList) {
                primesArray[i++] = p;
            }
        return primesArray;
    }

    public void shutdown() {
        ex.shutdown();
    }

    public int getPrimesCount() {
        int count = 0;
        for (int[] plist : primes)
            count += plist.length;
        return count;
    }

    private void updatePrimes(Future<int[]> future) {
        int[] newPrimes = null;
        try {
            newPrimes = future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        assert newPrimes != null;
        primes.add(newPrimes);
    }

    private void updatePrimes(Collection<Future<int[]>> futures) {
        for (Future<int[]> future : futures) {
            try {
                primes.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private int countPrimes(int from, int to) {
        int primeCounter = 0;
        if (from % 2 == 0)
            from += 1;
        for (int i = from; i < to; i += 2) {
            if (isPrime(i)) {
                primeCounter++;
            }
        }
        return primeCounter;
    }

    private int[] collectPrimes(int[] primeList, int from, int to) {
        int j = 0;
        if (from % 2 == 0)
            from += 1;
        for (int i = from; i < to; i += 2) {
            if (isPrime(i)) {
                primeList[j++] = i;
            }
        }
        return primeList;
    }

    private void traverse(int p, int from, int to) {
        int i = p + 2 * p * (((from - p) + (2 * p) - 1) / (2 * p));

        while (i < to) {
            flip(i);
            i += p * 2;
        }
    }

    public int[] initParallel() {
        if (n <= 2)
            return new int[0];
        else if (n == 3)
            return new int[] {2};

        this.initParallel(n);
        primes.add(0, new int[] {2});
        return convertToArray();
    }

    private void initParallel(int to) {

        int from = ( (int) Math.sqrt(to) ) + 1;
        if (to > 24) {
            initParallel(from);
        } else {
            from = 3;
        }

        int firstByteOccurrence = ((from + 15) / 16) * 16;
        int lastByteOccurrence = ((to - 1) / 16) * 16;
        int bytesPerThread = ((lastByteOccurrence - firstByteOccurrence) / 16) / nThreads;

        if (bytesPerThread <= 0) {
            Future<int[]> fut = ex.submit(new Task(from, to));
            updatePrimes(fut);

        } else {
            List<Task> tasks = new ArrayList<>();

            tasks.add(new Task(from, firstByteOccurrence + bytesPerThread * 16));
            for (int i = 1; i < nThreads - 1; i++)
                tasks.add(new Task(
                        firstByteOccurrence + bytesPerThread * 16 * i,
                        firstByteOccurrence + bytesPerThread * 16 * (i + 1))
                );
            tasks.add(new Task(firstByteOccurrence + bytesPerThread * 16 * (nThreads - 1), to));

            Collection<Future<int[]>> futureList = null;
            try {
                futureList = ex.invokeAll(tasks);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assert futureList != null;
            updatePrimes(futureList);
        }
    }

    private class Task implements Callable<int[]> {
        int from;
        int to;
        int[] newPrimes;

        Task(int from, int to) {
            this.to = to;
            this.from = from;
        }

        @Override
        public int[] call() {
            for (int[] list : primes)
                for (int p : list)
                    traverse(p, from, to);
            newPrimes = new int[countPrimes(from, to)];
            collectPrimes(newPrimes, from, to);
            // System.out.println(Thread.currentThread().getName() + " FROM: " + from + " TO: " + to + " is done. " + newPrimes.length);
            return newPrimes;
        }
    }

    private static boolean testSieve(int n, int nThreads) {
        SequentialSieve s = new SequentialSieve(n);
        ParallelSieve p = new ParallelSieve(n, nThreads);

        int[] seqPrimes = s.startPrimeFinding();
        int[] parPrimes = p.initParallel();

        p.shutdown();

        if (seqPrimes.length != parPrimes.length) {
            return false;
        }

        for (int i = 0; i < seqPrimes.length; i++) {
            if (seqPrimes[i] != parPrimes[i])
                return false;
        }
        return true;
    }

    public static void main(String[] args) {
        if (!(ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-ea"))) {
            System.out.println("IMPORTANT: Include -ea as flag to run the test");
            return;
        }

        if(!(args.length == 2)) {
            System.out.print("Invalid number of arguments!\nExpected 2, got " + (args.length));
            return;
        }
        int n;
        int nThreads;
        try {
            n = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Argument 1 must be integer");
            return;
        }
        if (n > 2147391111) {
            System.out.println("n is too high. Only equals or less than 2147391111 is allowed.");
            return;
        }
        try {
            nThreads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Argument 2 must be integer!");
            return;
        }
        assert testSieve(n, nThreads);
        System.out.println("Test complete. No errors.");
    }
}
