import java.util.concurrent.locks.ReentrantLock;

public class FactorMonitor {
    private ReentrantLock lock = new ReentrantLock();
    private volatile long n;

    public FactorMonitor(long n) {
        this.n = n;
    }

    public long getN() {return n;}

    public void divideNumberByPrime(int p) {
        lock.lock();
        try {
            n /= p;
        } finally {
            lock.unlock();
        }
    }
}
