package com.snowcattle.game.common.util;

/**
 * Created by jiangwenping on 17/2/6.
 */
public final class RandomStringGeneratorTest {
    @org.junit.Test
    public void legacyMain() {
       String string =  new RandomStringGenerator().generateRandomString(10);
        System.out.println(string);
    }
}
