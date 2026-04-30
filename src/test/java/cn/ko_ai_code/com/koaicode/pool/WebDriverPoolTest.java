package cn.ko_ai_code.com.koaicode.pool;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebDriverPool 单元测试
 *
 * @author ko
 */
class WebDriverPoolTest {

    private TestableWebDriverPool pool;

    @BeforeEach
    void setUp() {
        pool = new TestableWebDriverPool();
        pool.init();
    }

    @AfterEach
    void tearDown() {
        pool.destroy();
    }

    @Test
    void shouldBorrowAllDriversWithoutBlocking() throws Exception {
        WebDriver d1 = pool.borrow();
        WebDriver d2 = pool.borrow();
        WebDriver d3 = pool.borrow();

        Assertions.assertThat(d1).isNotNull();
        Assertions.assertThat(d2).isNotNull();
        Assertions.assertThat(d3).isNotNull();
        Assertions.assertThat(pool.availableCount()).isZero();

        pool.returnDriver(d1);
        pool.returnDriver(d2);
        pool.returnDriver(d3);
    }

    @Test
    void shouldBlockWhenPoolExhausted() throws Exception {
        WebDriver d1 = pool.borrow();
        WebDriver d2 = pool.borrow();
        WebDriver d3 = pool.borrow();
        Assertions.assertThat(pool.availableCount()).isZero();

        AtomicBoolean blocked = new AtomicBoolean(false);
        AtomicBoolean acquired = new AtomicBoolean(false);
        CountDownLatch blockerStarted = new CountDownLatch(1);

        Thread borrower4 = new Thread(() -> {
            try {
                blockerStarted.countDown();
                blocked.set(true);
                WebDriver d = pool.borrow();
                acquired.set(true);
                pool.returnDriver(d);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        borrower4.start();

        blockerStarted.await();
        Thread.sleep(200);
        Assertions.assertThat(blocked).isTrue();
        Assertions.assertThat(acquired).isFalse();

        pool.returnDriver(d1);
        borrower4.join(2000);

        Assertions.assertThat(acquired).isTrue();

        pool.returnDriver(d2);
        pool.returnDriver(d3);
    }

    @Test
    void shouldReuseReturnedDriver() throws Exception {
        WebDriver d1 = pool.borrow();
        WebDriver d2 = pool.borrow();
        WebDriver d3 = pool.borrow();

        pool.returnDriver(d1);
        pool.returnDriver(d2);

        WebDriver borrowedAgain = pool.borrow();
        Assertions.assertThat(borrowedAgain).isSameAs(d1);

        WebDriver borrowedAgain2 = pool.borrow();
        Assertions.assertThat(borrowedAgain2).isSameAs(d2);

        pool.returnDriver(borrowedAgain);
        pool.returnDriver(borrowedAgain2);
        pool.returnDriver(d3);
    }

    @Test
    void shouldNotLeakUnderConcurrentLoad() throws Exception {
        int numThreads = 6;
        int cyclesPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger successfulBorrows = new AtomicInteger(0);
        AtomicInteger successfulReturns = new AtomicInteger(0);
        CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startBarrier.await();
                    for (int j = 0; j < cyclesPerThread; j++) {
                        WebDriver driver = pool.borrow();
                        successfulBorrows.incrementAndGet();
                        Thread.sleep(10);
                        pool.returnDriver(driver);
                        successfulReturns.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        Assertions.assertThat(completed).isTrue();
        Assertions.assertThat(errors).isEmpty();
        Assertions.assertThat(successfulBorrows.get()).isEqualTo(numThreads * cyclesPerThread);
        Assertions.assertThat(successfulReturns.get()).isEqualTo(numThreads * cyclesPerThread);
        Assertions.assertThat(pool.availableCount()).isEqualTo(3);
    }

    @Test
    void shouldNotDeadlockUnderHighContention() throws Exception {
        int numThreads = 10;
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger borrowCount = new AtomicInteger(0);
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    for (int j = 0; j < 50; j++) {
                        WebDriver driver = pool.borrow();
                        borrowCount.incrementAndGet();
                        Thread.sleep(1);
                        pool.returnDriver(driver);
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Assertions.assertThat(completed).isTrue();
        Assertions.assertThat(errors).isEmpty();
        Assertions.assertThat(borrowCount.get()).isEqualTo(numThreads * 50);
    }

    @Test
    void shouldThrowWhenInterruptedWhileWaiting() throws Exception {
        WebDriver d1 = pool.borrow();
        WebDriver d2 = pool.borrow();
        WebDriver d3 = pool.borrow();

        AtomicReference<Exception> caught = new AtomicReference<>();
        Thread borrower = new Thread(() -> {
            try {
                pool.borrow();
            } catch (Exception e) {
                caught.set(e);
            }
        });
        borrower.start();
        Thread.sleep(200);
        Assertions.assertThat(borrower.isAlive()).isTrue();

        borrower.interrupt();
        borrower.join(2000);

        Assertions.assertThat(caught.get()).isInstanceOf(InterruptedException.class);

        pool.returnDriver(d1);
        pool.returnDriver(d2);
        pool.returnDriver(d3);
    }

    @Test
    void shouldReturnAllDriversToPoolAfterBorrowAndReturn() throws Exception {
        WebDriver d1 = pool.borrow();
        WebDriver d2 = pool.borrow();
        WebDriver d3 = pool.borrow();

        Assertions.assertThat(pool.availableCount()).isZero();

        pool.returnDriver(d1);
        pool.returnDriver(d2);
        pool.returnDriver(d3);

        Assertions.assertThat(pool.availableCount()).isEqualTo(3);

        pool.borrow();
        pool.borrow();
        pool.borrow();

        Assertions.assertThat(pool.availableCount()).isZero();
    }

    /**
     * 测试用子类，通过返回 Mockito mock 避免实际初始化 ChromeDriver。
     */
    static class TestableWebDriverPool extends WebDriverPool {

        @Override
        protected WebDriver createDriver(int width, int height) {
            return Mockito.mock(WebDriver.class);
        }
    }
}
