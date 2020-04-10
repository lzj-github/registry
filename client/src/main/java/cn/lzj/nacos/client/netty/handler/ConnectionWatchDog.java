package cn.lzj.nacos.client.netty.handler;

import cn.lzj.nacos.client.config.DiscoveryProperties;
import cn.lzj.nacos.client.naming.NamingServiceImpl;
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

import java.util.Random;
import java.util.concurrent.TimeUnit;


@Slf4j
@Data
@ChannelHandler.Sharable
abstract public class ConnectionWatchDog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

    private  Bootstrap bootstrap;
    private  Timer timer;
    private volatile boolean reconnect = true;
    private int attempts;//重试次数

    private int[] reconnectGapTime={1,2,3,4,5,10,15,30,60,120,180,300};

    private DiscoveryProperties discoveryProperties;

    private NamingServiceImpl nacosNamingService;


    public ConnectionWatchDog(Bootstrap bootstrap, Timer timer, DiscoveryProperties discoveryProperties, NamingServiceImpl nacosNamingService, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.discoveryProperties = discoveryProperties;
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
            if(attempts>=5){
                //重连了5次该server还是失败的话，就从server集群中选取其中一个来连接
                //先把该server从可选的nettyServers去掉
                NettyClient.nettyServers.remove(NettyClient.nettyServer);
                String serverIp = NettyClient.getKeyByValue(NettyClient.nettyServer);
                NettyClient.servers.remove(serverIp);
                NettyClient.mappingMap.remove(serverIp);
                Random random=new Random(System.currentTimeMillis());
                //重新选取一个server来来连接，而且在client存的可用server列表还有server的情况下
                if(NettyClient.nettyServers.size()>0){
                    NettyClient.index=random.nextInt(NettyClient.nettyServers.size());
                    NettyClient.nettyServer=NettyClient.nettyServers.get(NettyClient.index);
                    attempts=0;
                    log.warn("尝试连接"+NettyClient.getKeyByValue(NettyClient.nettyServer));
                }else{
                    log.error("没有可用的server可以尝试来连接，请通知服务器管理员或者尝试重新启动");
                }
            }
            if (attempts < 12) {
                //重连的间隔时间会越来越长
                //正常来说，假如重连的server都不可用，那么只有对NettyClient.nettyServer列表中最后一个才会用到attempts中后面的几个重试时间
                //其他都是在5那里就重新置0了
                int timeout = reconnectGapTime[attempts];
                timer.newTimeout(this, timeout, TimeUnit.SECONDS);
                attempts++;
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
        ChannelFuture channelFuture = bootstrap.connect(NettyClient.nettyServer.split(":")[0], Integer.parseInt(NettyClient.nettyServer.split(":")[1]));
        channelFuture.addListener(new ConnectionListener()).sync();
        log.info("重新连接上了"+NettyClient.getKeyByValue(NettyClient.nettyServer));
        //重新连接后channel会改变
        NettyClient.channel=channelFuture.channel();
        //System.out.println("channel:"+NettyClient.channel);

        //因为防止只有一个server节点，所以重新注册服务，要去重!!!
        nacosNamingService.registerInstance(discoveryProperties.getService(), discoveryProperties.getClusterName(),
                discoveryProperties.getClientIp(), discoveryProperties.getClientPort(), discoveryProperties.getNamespace());

    }





}
