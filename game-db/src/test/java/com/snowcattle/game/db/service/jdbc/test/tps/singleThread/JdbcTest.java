package com.snowcattle.game.db.service.jdbc.test.tps.singleThread;

import com.snowcattle.game.db.service.common.uuid.SnowFlakeUUIDService;
import com.snowcattle.game.db.service.jdbc.entity.Order;
import com.snowcattle.game.db.service.jdbc.service.entity.impl.OrderService;
import com.snowcattle.game.db.service.jdbc.test.TestConstants;
import org.junit.Assert;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by jiangwenping on 17/3/20.
 * <p>
 * {@link #legacyMain()} 仅校验容器与 Bean；批量插入压测请本机起库后调用 {@link #insertTest}。
 */
public final class JdbcTest {

    @org.junit.Test
    public void legacyMain() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"bean/*.xml"});
        try {
            OrderService orderService = getOrderService(ctx);
            SnowFlakeUUIDService snowFlakeUUIDService = ctx.getBean(SnowFlakeUUIDService.class);
            snowFlakeUUIDService.setNodeId(1);
            Assert.assertNotNull(orderService);
            Assert.assertNotNull(snowFlakeUUIDService);
        } finally {
            ctx.close();
        }
    }

    public static void insertTest(ClassPathXmlApplicationContext classPathXmlApplicationContext, OrderService orderService, SnowFlakeUUIDService snowFlakeUUIDService) {

        int startSize = TestConstants.batchStart;
        int endSize = TestConstants.batchStart+TestConstants.saveSize;

        long start = System.currentTimeMillis();
        for (int i = startSize; i < endSize; i++) {
            Order order = new Order();
            order.setUserId(TestConstants.userId);
            order.setId(snowFlakeUUIDService.nextId());
            order.setStatus("测试插入" + i);
            orderService.insertOrder(order);
        }
        long end  = System.currentTimeMillis();

        long time = end - start;
        System.out.println("存储" + TestConstants.saveSize + "耗时" + time);

    }

    public static OrderService getOrderService(ClassPathXmlApplicationContext classPathXmlApplicationContext) {
        OrderService orderService = (OrderService) classPathXmlApplicationContext.getBean("orderService");
        return orderService;
    }
}
