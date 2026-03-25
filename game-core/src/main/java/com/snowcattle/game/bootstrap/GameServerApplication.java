package com.snowcattle.game.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 游戏服务器Spring Boot启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.snowcattle.game"})
public class GameServerApplication {

    public static void main(String[] args) {
        System.out.println("Starting Netty Game Server with Spring Boot...");
        SpringApplication.run(GameServerApplication.class, args);
        System.out.println("Netty Game Server started successfully!");
    }
}