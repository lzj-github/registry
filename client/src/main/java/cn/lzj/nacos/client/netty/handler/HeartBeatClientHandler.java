package cn.lzj.nacos.client.netty.handler;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.client.config.NacosDiscoveryProperties;
import cn.lzj.nacos.client.netty.MessageProtocol;
import cn.lzj.nacos.client.netty.NettyClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {


    @Autowired
    private NettyClient nettyClient;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("激活时间是："+new Date());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("停止时间是："+new Date());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        String msgStr=new String(msg.getContent());
        if(msgStr.startsWith(Constants.DISCONNECT_ROUND)&& msgStr.endsWith(Constants.DISCONNECT_ROUND)){
            log.error("客户端接收到服务端传来的消息:"+getRealMsg(msgStr));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    //去除协议字符
    private String getRealMsg(String msg){
        return msg.substring(Constants.PROTOCOL_LEN,msg.length()-Constants.PROTOCOL_LEN);
    }

}
