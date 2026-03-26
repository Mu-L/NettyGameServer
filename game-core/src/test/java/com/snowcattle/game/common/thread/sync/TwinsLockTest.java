package com.snowcattle.game.common.thread.sync;

import java.util.concurrent.locks.Lock;

/**
 * Created by jiangwenping on 17/3/7.
 */
public final class TwinsLockTest {

    @org.junit.Test
    public void legacyMain() {
//        final Lock lock = new TwinsLock();
        final Lock lock = new SingleLock();
        class Worker extends Thread {
            public void run() {
                for (int round = 0; round < 3; round++) {
                    lock.lock();

                    try {
                        Thread.sleep(100L);
                        System.out.println(Thread.currentThread());
                        Thread.sleep(100L);
                    } catch (Exception ex) {

                    } finally {
                        lock.unlock();
                    }
                }
            }
        }

        for (int i = 0; i <10; i++) {
            Worker w = new Worker();
            w.start();
        }

        new Thread(() -> {
            for (int i = 0; i < 30; i++) {
                try {
                    Thread.sleep(200L);
                    System.out.println();
                } catch (Exception ex) {

                }
            }
        }).start();

        try {
            Thread.sleep(8000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}