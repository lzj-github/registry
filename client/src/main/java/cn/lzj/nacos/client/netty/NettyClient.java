package cn.lzj.nacos.client.netty;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.client.config.DiscoveryProperties;
import cn.lzj.nacos.client.core.HostReactor;
import cn.lzj.nacos.client.naming.NacosNamingService;
import cn.lzj.nacos.client.netty.handler.ConnectionWatchDog;
import cn.lzj.nacos.client.netty.handler.HeartBeatClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
@Component
public class NettyClient {

    private static volatile NettyClient nettyClient = null;

    private EventLoopGroup group;

    public static Channel channel;

    private Bootstrap bootstrap;

    private ChannelFuture channelFuture;

    private String ip;

    private int port;

    //得到一个延迟队列实例
    private final HashedWheelTimer timer = new HashedWheelTimer();

    @Autowired
    private DiscoveryProperties discoveryProperties;

    @Autowired
    private NacosNamingService nacosNamingService;

    @Autowired
    private final ConnectorIdleStateTrigger idleStateTrigger;

    @Autowired
    private HostReactor hostReactor;

    public void start(CountDownLatch countDownLatch) {
        try {
            ip= discoveryProperties.getNettyServerIp();
            port= discoveryProperties.getNettyServerPort();
            //客户端需要一个事件循环组
            group = new NioEventLoopGroup();
            //创建客户端启动对象
            bootstrap = new Bootstrap();
            //设置相关参数
            bootstrap.group(group)//设置线程组
                    .channel(NioSocketChannel.class);// 使用NioSocketChannel作为客户端的通道实现

            //手动传参，不然报空指针！！！
            final ConnectionWatchDog watchDog=new ConnectionWatchDog(bootstrap,timer, discoveryProperties,nacosNamingService,true) {
                @Override
                public ChannelHandler[] handlers() {
                    return new ChannelHandler[]{
                            new MessageEncoder(),//编码器
                            this,
                            new IdleStateHandler(0, Constants.DEFAULT_SEND_HEART_BEAT_INTERVAL,0, TimeUnit.SECONDS),
                            idleStateTrigger,
                            new MessageDecoder(),//解码器
                            new HeartBeatClientHandler(hostReactor)
                    };
                }
            };

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //加入处理器
                     ChannelPipeline pipeline = ch.pipeline();
                     pipeline.addLast(watchDog.handlers());
                }
            });

            //启动客户端去连接服务器端,添加启动重连监听器
            // 通过sync方法同步等待通道关闭处理完毕，这里会阻塞等待通道关闭完成
            channelFuture = bootstrap.connect(ip, port).addListener(new ConnectionListener()).sync();
            //得到 channel
            channel = channelFuture.channel();
            log.info("连接服务器成功...");
            countDownLatch.countDown();
        } catch (InterruptedException e) {
            log.error("客户端连接服务器失败");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            //对通道关闭进行监听
            channelFuture.channel().close().sync();
            group.shutdownGracefully();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
