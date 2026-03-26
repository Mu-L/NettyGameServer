package com.snowcattle.game.bootstrap;

import com.snowcattle.game.common.constant.Loggers;
import com.snowcattle.game.common.util.BeanUtil;
import com.snowcattle.game.service.order.OrderCacheDbService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "com.snowcattle.game")
public class GameServerBootApplication implements CommandLineRunner {

    private final ConfigurableApplicationContext applicationContext;

    public GameServerBootApplication(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication.run(GameServerBootApplication.class, args);
    }

    @Override
    public void run(String... args) {
        GameServer gameServer = new GameServer();
        gameServer.setApplicationContext(applicationContext);
        gameServer.startServer();

//        // insert order (db + redis cache)
//        try {
//            OrderCacheDbService orderCacheDbService = (OrderCacheDbService) BeanUtil.getBean("orderCacheDbService");
//            long playerId = 3;
//            long orderId = System.currentTimeMillis();
//            String status = "online_login";
//            orderCacheDbService.insertOrderWithCache(playerId, orderId, status);
//        } catch (Exception e) {
//            Loggers.serverLogger.error("insert order cache failed, playerId={}", 3, e);
//        }
    }
}
