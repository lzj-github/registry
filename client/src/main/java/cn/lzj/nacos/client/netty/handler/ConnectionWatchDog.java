package cn.lzj.nacos.client.netty.handler;

import cn.lzj.nacos.client.config.NacosDiscoveryProperties;
import cn.lzj.nacos.client.naming.NacosNamingService;
import cn.lzj.nacos.client.netty.ChannelHandlerHolder;
import cn.lzj.nacos.client.netty.ConnectionListener;
import cn.lzj.nacos.client.netty.NettyClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Slf4j
@Data
@ChannelHandler.Sharable
abstract public class ConnectionWatchDog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

    private  Bootstrap bootstrap;
    private  Timer timer;
    private volatile boolean reconnect = true;
    private int attempts;//重试次数

    private NacosDiscoveryProperties nacosDiscoveryProperties;

    private NacosNamingService nacosNamingService;

    public ConnectionWatchDog(Bootstrap bootstrap, Timer timer,NacosDiscoveryProperties nacosDiscoveryProperties,NacosNamingService nacosNamingService,boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.nacosDiscoveryProperties=nacosDiscoveryProperties;
        this.nacosNamingService=nacosNamingService;
        this.reconnect = reconnect;
    }

    /**
     * channel链路每次active的时候，将其连接的次数重新置0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("当前链路已经激活了，重连尝试次数重新置为0...");
        attempts = 0;
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //掉线的channelInactive方法是从头节点往尾节点的顺序来执行
        log.error("掉线了...");
        if(reconnect){
            log.error("链接关闭，将进行重连...");
            if (attempts < 12) {
                attempts++;
                //重连的间隔时间会越来越长
                int timeout = 2 << attempts;
                timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
            }
        }
        ctx.fireChannelInactive();
    }


    @Override
    public void run(Timeout timeout) throws Exception {

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(handlers());
            }
        });
        //重连
        ChannelFuture channelFuture = bootstrap.connect(nacosDiscoveryProperties.getNettyServerIp(), nacosDiscoveryProperties.getNettyServerPort());
        channelFuture.addListener(new ConnectionListener()).sync();
        //重新连接后channel会改变
        NettyClient.channel=channelFuture.channel();
        //System.out.println("channel:"+NettyClient.channel);
        //重新注册服务
        nacosNamingService.registerInstance(nacosDiscoveryProperties.getService(),nacosDiscoveryProperties.getClusterName(),
                nacosDiscoveryProperties.getClientIp(),nacosDiscoveryProperties.getClientPort(),nacosDiscoveryProperties.getNamespace());

    }




}
