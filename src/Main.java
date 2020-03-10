import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Integer n;
        Integer sieveThreads;
        Integer factorThreads;
        boolean writeToFile = false;

        if(!(args.length >= 2 && args.length <= 4)) {
            System.out.print("Invalid number of arguments!\nExpected 2-4, got " + (args.length));
            return;
        } else if (args.length == 2) {
            n = parseInt(args, 0);
            sieveThreads = factorThreads = parseInt(args, 1);
        } else if (args.length == 3) {
            n = parseInt(args, 0);
            sieveThreads = parseInt(args, 1);
            factorThreads = parseInt(args, 2);
        } else {
            n = parseInt(args, 0);
            sieveThreads = parseInt(args, 1);
            factorThreads = parseInt(args,2);
            writeToFile = Boolean.parseBoolean(args[3]);
        }

        if (n == null || sieveThreads == null || factorThreads == null) {
            return;
        }
        System.out.println("Running program with: ");
        System.out.println(" -------------------------- n: " + n);
        System.out.println(" ---------- Threads for sieve: " + sieveThreads);
        System.out.println(" -- Threads for factorisation: " + factorThreads);
        System.out.println(" -------------- Write to file: " + writeToFile);
        System.out.println();


        System.out.println("All times in nanoseconds");

        int[] primes;
        List<Object> parSieveResult;
        long seqSieveTime;
        long parSieveTime;
        long seqFactorTime;
        long parFactorTime;

        seqSieveTime = timeSeqSieve(n, 7);
        System.out.println(" -- Sequential Sieve: " + seqSieveTime);

        parSieveResult = timeParSieve(n, 512, 7);
        parSieveTime = (long) parSieveResult.get(0);
        primes = (int[]) parSieveResult.get(1);
        System.out.println(" ---- Parallel Sieve: " + parSieveTime);
        System.out.println(" ----- Sieve Speedup: " + ((double) seqSieveTime / (double) parSieveTime));
        System.out.println();

        seqFactorTime = timeSeqFactorise(n, 100, primes, 7, false);
        System.out.println(" - Sequential factor: " + seqFactorTime);

        /*
        ParallelSieve p = new ParallelSieve(n, 512);
        primes = p.initParallel();
        p.shutdown();
         */

        parFactorTime = timeParFactorise(n, 100, primes, factorThreads, 7, false);
        System.out.println(" --- Parallel factor: " + parFactorTime);
        System.out.println(" ---- Factor Speedup: " + ((double) seqFactorTime / (double) parFactorTime));
    }

    private static long timeSeqSieve(int N, int iterations) {
        int[] primes = null;
        long[] timeList = new long[iterations];

        SequentialSieve seqSieve;
        for (int i = 0; i < iterations; i++) {

            seqSieve = new SequentialSieve(N);
            long start = System.nanoTime();
            primes = seqSieve.startPrimeFinding();
            long end = System.nanoTime();

            timeList[i] = end - start;
        }
        return median(timeList);
    }

    private static List<Object> timeParSieve(int N, int nThreads, int iterations) {
        int[] primes = null;
        long[] timeList = new long[iterations];

        ParallelSieve parSieve;

        for (int i = 0; i < iterations; i++) {
            parSieve = new ParallelSieve(N, nThreads);
            long start = System.nanoTime();
            primes = parSieve.initParallel();
            long end = System.nanoTime();

            timeList[i] = end - start;
            parSieve.shutdown();
        }
        return Arrays.asList(median(timeList), primes);
    }

    private static long timeSeqFactorise(int N, int K, int[] primes, int iterations, boolean writeToFile) {
        long[] timeList = new long[iterations];

        SequentialPrimeFactorisation seq = new SequentialPrimeFactorisation(primes);
        long NSquared = (long) N * (long) N;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            for (long n = NSquared - K; n < NSquared; n++) {
                seq.factorise(n);
            }
            long end = System.nanoTime();

            timeList[i] = end - start;
        }

        Oblig3Precode o3p = new Oblig3Precode(N * N);
        List<Long> solution;
        if (writeToFile) {
            for (long n = NSquared - K; n < NSquared; n++) {
                solution = seq.factorise(n);
                for (long factor : solution) {
                    o3p.addFactor(n, factor);
                }
            }
            o3p.writeFactors();
        }
        return median(timeList);
    }

    private static long timeParFactorise(int N, int K, int[] primes, int nThreads, int iterations, boolean writeToFile) {
        long[] timeList = new long[iterations];

        ParallelPrimeFactorisation pf = new ParallelPrimeFactorisation(primes, nThreads);
        long NSquared = (long) N * (long) N;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            for (long n = NSquared - K; n < NSquared; n++) {
                pf.factorise(n);
            }
            long end = System.nanoTime();

            timeList[i] = end - start;
        }

        Oblig3Precode o3p = new Oblig3Precode(N * N);
        List<Long> solution;
        if (writeToFile) {
            for (long n = NSquared - K; n < NSquared; n++) {
                solution = pf.factorise(n);
                for (long factor : solution) {
                    o3p.addFactor(n, factor);
                }
            }
            o3p.writeFactors();
        }
        pf.shutdown();
        return median(timeList);
    }

    private static long median(long[] a) {
        Arrays.sort(a);
        long median;
        if (a.length % 2 == 0)
            median = (a[a.length/2] + a[a.length/2 - 1])/2;
        else
            median = a[a.length/2];
        return median;
    }

    private static Integer parseInt(String[] args, int index) {
        Integer n = null;
        try {
            n = Integer.parseInt(args[index]);
        } catch(NumberFormatException e){
            System.out.println("Argument " + (index + 1) + " has to be an integer!");
        }
        return n;
    }
}
