package com.snowcattle.game.bootstrap;

import com.snowcattle.game.common.util.Assert;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by jiangwenping on 16/11/18.
 */
public class ShutdownHook implements Runnable {

    ConfigurableApplicationContext applicationContext;

    public ShutdownHook(ConfigurableApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "The 'beanfactory' argument must not be null.");
        this.applicationContext = applicationContext;
    }

    public void run() {  //重写Runnable中的run方法并在此销毁bean
        this.applicationContext.close();
    }
}
