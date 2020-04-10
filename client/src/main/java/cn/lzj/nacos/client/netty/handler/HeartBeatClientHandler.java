package cn.lzj.nacos.client.netty.handler;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.ServiceInfo;
import cn.lzj.nacos.client.config.DiscoveryProperties;
import cn.lzj.nacos.client.core.HostReactor;
import cn.lzj.nacos.client.netty.MessageProtocol;
import cn.lzj.nacos.client.netty.NettyClient;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.ws.Service;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {



    private HostReactor hostReactor;

    public HeartBeatClientHandler(HostReactor hostReactor) {
        this.hostReactor=hostReactor;
    }

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
            log.error("客户端接收到服务端传来断开连接的消息:"+getRealMsg(msgStr));
        }else if(msgStr.startsWith(Constants.SERVICE_FOUND_ROUND)&&msgStr.endsWith(Constants.SERVICE_FOUND_ROUND)){
            Map<String, ServiceInfo> services= JSON.parseObject(getRealMsg(msgStr),new TypeReference<Map<String,ServiceInfo>>(){});
            //不能这样转 JSON.parseObject(getRealMsg(msgStr), ConcurrentHashMap.class);
            log.info("收到服务端传来的服务列表:"+services);
            hostReactor.putService(services);
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
