package com.snowcattle.game.executor;

import java.util.concurrent.locks.LockSupport;

/**
 * Created by jiangwenping on 17/1/6.
 */
public final class TestLockSupport {
    private TestLockSupport() {
    }

    @org.junit.Test
    public void legacyMain() {

//        test();
        testCpu();
    }

    public static void testCpu(){
        long startTime = System.currentTimeMillis();
        System.out.println(System.currentTimeMillis());
        int number = 0;
        int prints = 0;
        while (prints < 5) {
            LockSupport.unpark(Thread.currentThread());
            ++number;
            long currTime = System.currentTimeMillis();
            if(number % 10000 == 0) {
                LockSupport.park();
                System.out.println("运行次数" + number + "时间" + (currTime - startTime));
                prints++;
            }
        }
    }

    public static void  test(){
        System.out.println("unpark startup");
        LockSupport.unpark(Thread.currentThread());
//        LockSupport.unpark(Thread.currentThread());
        System.out.println("park1");
        LockSupport.park();

//        System.out.println("park2");
//        LockSupport.park();
        System.out.println("running");

    }
}
