import java.util.*;
import java.util.concurrent.*;

public class SequentialPrimeFactorisation {
    final int[] primes;

    public SequentialPrimeFactorisation(int[] primes) {
        this.primes = primes;
    }

    public List<Long> factorise(long n) {
        List<Long> solution = new ArrayList<>();
        for (int p : primes) {
            if (Math.pow(p, 2) > n)
                break;

            while (n % p == 0) {
                solution.add((long) p);
                n /= p;
            }
            if (n == 1)
                break;
        }
        if (n != 1)
            solution.add(n);
        return solution;
    }
}
