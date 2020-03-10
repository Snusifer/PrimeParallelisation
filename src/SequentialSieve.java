import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Sequential solution to the Sieve of Eratosthenes
 *
 * @author Kim Sverre Hilton <kimsh@ifi.uio.no> for the course IN3030 at UiO, Spring 2020
 *
 * Recreated from original code by:
 * @author Magnus Espeland <magnuesp@ifi.uio.no> for the course IN3030 at UiO, Spring 2019
 *
 * which can be found at:
 * https://github.uio.no/magnuesp/IN3030-v19/blob/master/magnuesp/Sieve/Sieve.java
 *
 * */


public class SequentialSieve {

    int n;
    int squaredN;
    int primeCount;
    byte[] bytes;


    /**
     * We can exclude n / 2 numbers because even numbers. room for 8 bits (numbers) per byte. 8 * 2 = 16 cells.
     * By using a byte[] instead of for instance an int[], we can cut down n by a factor of 16.
     * This is because 1) we exclude all even numbers (except 2) and 2) one cell (index) in a byte[] can represent 8 numbers
     * .. so n / 16.
     * + 1 because it's integer division, integers round down.
     * */
    public SequentialSieve(int n){
        this.n = n;
        squaredN = (int) Math.sqrt(n);
        bytes = new byte[n / 16 + 1];   // add 1 because of Integer rounding
    }


    /**
     * 3 steps here:
     *  1st step is to iterate through all primes < square root of n, and mark subsequent non-primes
     *  starting with prime number i^2, and increment with 2*i
     *  also, remember to increment primeCount - count all primes before squaredN
     *
     *  2nd step is to count all primes after squaredN
     *
     *  3rd step is to collect all primes in an array or something
     * */
    int[] startPrimeFinding(){
        sieveUpToSquared(); // sieve out all non-primes by using primes up to squaredN
        countPrimesAfterSquared(); // count primes after squaredN
        //System.out.println("\n-----No. of primes: " + primeCount);
        return collectPrimes(); // add all primes to a list
    }


    void sieveUpToSquared(){

        int currentPrime = 3;
        primeCount++;

        while(currentPrime <= squaredN){
            traverse(currentPrime);
            currentPrime = findNextPrime(currentPrime + 2);
            primeCount++;
        }
    }

    void countPrimesAfterSquared(){

        int startAt;

        if ((squaredN + 1) % 2 == 0){
            startAt = squaredN + 2;
        } else {
            startAt = squaredN + 1;
        }

        int currentPrime = findNextPrime(startAt);

        while (currentPrime != 0){
            primeCount++;
            currentPrime = findNextPrime(currentPrime + 2);
        }
    }


    int[] collectPrimes(){

        int[] primes = new int[primeCount];
        primes[0] = 2;
        int currentPrime = 3;

        for (int i = 1; i < primeCount; i++){
            primes[i] = currentPrime;
            currentPrime = findNextPrime(currentPrime + 2);
        }
        return primes;
    }

    void traverse(int p){
        for (int i = p * p; i < n; i += p * 2){
            flip(i);
        }
    }

    void flip(int i){
        int byteCell = i / 16;  // find respective cell (index) in byte[] bytes
        int bit = (i / 2) % 8;  // find respective bit within byteIndex

        /*
         * 1 << bit shifts a binary 1 bit number of spaces to the left in the byte.
         * 1 << 0 = 0001
         * 1 << 1 = 0010
         * 1 << 2 = 0100
         * 1 << 3 = 1000, etc.
         *
         * | is the OR operand where we basically switch the relevant bit to a 1.
         * |= works similarly to +=
         * */
        bytes[byteCell] |= (1 << bit);
    }


    boolean isPrime(int i){

        int byteCell = i / 16;
        int bit = (i / 2) % 8;

        /*
         * see bit shift explanation in flip(), same thing here.
         * difference is instead of OR we have an AND operand, which in this case is used to compare
         * your respective bit to a flipped bit, meaning it returns 0 if it hasn't been flipped (is a prime)
         * and > 0 if it has been flipped.
         * */
        return (bytes[byteCell] & (1 << bit)) == 0;
    }


    int findNextPrime(int p){
        for (int i = p; i < n; i += 2){
            if (isPrime(i))
                return i;
        }
        return 0;
    }
}