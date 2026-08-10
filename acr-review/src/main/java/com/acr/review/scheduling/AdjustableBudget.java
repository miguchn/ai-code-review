package com.acr.review.scheduling;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 运行期可调上限的有界名额。上限每次 tryAcquire 时读取，避免重建 Semaphore。
 */
final class AdjustableBudget
{
    private final AtomicInteger held = new AtomicInteger();
    private final AtomicLong rejected = new AtomicLong();

    boolean tryAcquire(int maxPermits)
    {
        int limit = Math.max(0, maxPermits);
        if (limit == 0)
        {
            rejected.incrementAndGet();
            return false;
        }
        while (true)
        {
            int current = held.get();
            if (current >= limit)
            {
                rejected.incrementAndGet();
                return false;
            }
            if (held.compareAndSet(current, current + 1))
            {
                return true;
            }
        }
    }

    void release()
    {
        held.updateAndGet(value -> Math.max(0, value - 1));
    }

    void recordReject()
    {
        rejected.incrementAndGet();
    }

    int held()
    {
        return held.get();
    }

    long rejected()
    {
        return rejected.get();
    }
}
