package com.snowcattle.game.common.udp.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by jiangwenping on 17/1/22.
 */
public final class JavaUDPServer {
    @org.junit.Test
    public void legacyMain()throws Exception {
        String str_send = "Hello UDPclient";
        byte[] buf = new byte[1024];
        //服务端在3000端口监听接收到的数据
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(9999);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        //接收从客户端发送过来的数据（JUnit：单次接收或超时退出，避免死循环）
        DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
        System.out.println("server is on，waiting for client to send data......");
        ds.setSoTimeout(3000);
        try {
            ds.receive(dp_receive);
            System.out.println("server received data from client：");
            String str_receive = new String(dp_receive.getData(),0,dp_receive.getLength()) +
                                 " from " + dp_receive.getAddress().getHostAddress() + ':' + dp_receive.getPort();
            System.out.println(str_receive);
            DatagramPacket dp_send= new DatagramPacket(str_send.getBytes(),str_send.length(),dp_receive.getAddress(),9000);
            ds.send(dp_send);
        } catch (SocketTimeoutException e) {
            System.out.println("no UDP packet within timeout (ok for automated test)");
        } finally {
            ds.close();
        }
    }
}
