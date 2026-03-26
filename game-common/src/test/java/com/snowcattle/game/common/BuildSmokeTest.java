package com.snowcattle.game.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * 保证 {@code mvn test} 在含 test 源码的模块中至少执行一条 JUnit 用例（工程内大量 *Test 为 main 示例，不含 @Test）。
 */
public class BuildSmokeTest {

    @Test
    public void junitAndClasspathSmoke() {
        Assert.assertTrue(true);
    }
}
