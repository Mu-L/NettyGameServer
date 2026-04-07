package com.snowcattle.game.net.client.websocket;

import com.snowcattle.game.TestStartUp;
import com.snowcattle.game.bootstrap.manager.LocalMananger;
import com.snowcattle.game.service.config.GameServerConfigService;
import com.snowcattle.game.bootstrap.manager.spring.LocalSpringServiceManager;
import com.snowcattle.game.service.net.websocket.NetWebSocketServerConfig;
import com.snowcattle.game.service.net.websocket.SdWebSocketServerConfig;
import com.snowcattle.game.service.message.registry.MessageRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.Assert;

import java.net.URI;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Created by jiangwenping on 2017/11/8.
 */
public final class GameWebSocketClient {

    static final String URL = System.getProperty("url", "");
    private static final int RESPONSE_TIMEOUT_SECONDS = Integer.parseInt(System.getProperty("websocket.response.timeout.seconds", "10"));
    private static final int CONNECT_TIMEOUT_MILLIS = Integer.parseInt(System.getProperty("websocket.connect.timeout.millis", "5000"));
    private static final int SERVER_READY_TIMEOUT_MILLIS = Integer.parseInt(System.getProperty("websocket.server.ready.timeout.millis", "10000"));
    @org.junit.Test
    public void legacyMain() throws Exception  {
        try {
            TestStartUp.startUpWithSpring();
            LocalSpringServiceManager localSpringServiceManager = LocalMananger.getInstance().getLocalSpringServiceManager();
            localSpringServiceManager.setMessageRegistry(LocalMananger.getInstance().get(MessageRegistry.class));

            URI uri = URL.isEmpty() ? buildUriFromServerConfig() : new URI(URL);
            String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
            final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
            final int port;
            if (uri.getPort() == -1) {
                if ("ws".equalsIgnoreCase(scheme)) {
                    port = 80;
                } else if ("wss".equalsIgnoreCase(scheme)) {
                    port = 443;
                } else {
                    port = -1;
                }
            } else {
                port = uri.getPort();
            }

            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                System.err.println("Only WS(S) is supported.");
                return;
            }
            System.out.println("GameWebSocketClient config -> url=" + uri + ", scheme=" + scheme + ", ssl=" + "wss".equalsIgnoreCase(scheme) + ", host=" + host + ", port=" + port);
            Assert.assertTrue("websocket server not ready on " + host + ":" + port, waitForServerReady(host, port, SERVER_READY_TIMEOUT_MILLIS));

            final boolean ssl = "wss".equalsIgnoreCase(scheme);
            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } else {
                sslCtx = null;
            }

            EventLoopGroup group = new NioEventLoopGroup();
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final GameWebSocketClientHandler handler =
                    new GameWebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
//                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            Channel ch = b.connect(uri.getHost(), port).sync().channel();
            try {
                handler.handshakeFuture().sync();
                boolean received = handler.awaitLoginResponse(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                Assert.assertTrue("websocket login response timeout", received);
                Assert.assertTrue(
                        "websocket response decode failed, parsed type: " + handler.getParsedMessageType(),
                        handler.isLoginResponseReceived());
                ch.writeAndFlush(new CloseWebSocketFrame()).sync();
                ch.closeFuture().sync();
            } finally {
                if (ch.isOpen()) {
                    ch.close().syncUninterruptibly();
                }
                group.shutdownGracefully().syncUninterruptibly();
            }
        } finally {
            TestStartUp.stopWithSpring();
        }
    }

    private static boolean waitForServerReady(String host, int port, int timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket(host, port)) {
                return true;
            } catch (Exception ignore) {
                Thread.sleep(100L);
            }
        }
        return false;
    }

    private static URI buildUriFromServerConfig() throws Exception {
        GameServerConfigService configService = LocalMananger.getInstance().getLocalSpringServiceManager().getGameServerConfigService();
        NetWebSocketServerConfig webSocketServerConfig = configService.getNetWebSocketServerConfig();
        SdWebSocketServerConfig sdConfig = webSocketServerConfig == null ? null : webSocketServerConfig.getSdWebSocketServerConfig();
        String host = sdConfig == null ? "127.0.0.1" : sdConfig.getIp();
        int port = sdConfig == null ? 10300 : sdConfig.getPort();
        boolean ssl = sdConfig != null && sdConfig.isSsl();
        String scheme = ssl ? "wss" : "ws";
        return new URI(scheme + "://" + host + ":" + port + "/websocket");
    }
}
