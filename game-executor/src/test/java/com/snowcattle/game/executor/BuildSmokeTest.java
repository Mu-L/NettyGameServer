package com.snowcattle.game.executor;

import org.junit.Assert;
import org.junit.Test;

/**
 * 保证 {@code mvn test} 能发现并执行 JUnit 用例（大量 *Test 类为 main 示例程序）。
 */
public class BuildSmokeTest {

    @Test
    public void junitAndClasspathSmoke() {
        Assert.assertTrue(true);
    }
}
