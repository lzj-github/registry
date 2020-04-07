package cn.lzj.nacos.client.netty;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.BeatInfo;
import cn.lzj.nacos.client.config.NacosDiscoveryProperties;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.beans.BeanInfo;

@ChannelHandler.Sharable
@Slf4j
@Component
public class ConnectorIdleStateTrigger  extends SimpleChannelInboundHandler<MessageProtocol>  {

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState state=event.state();
            BeatInfo beatInfo=new BeatInfo();
            beatInfo.setNamespaceId(nacosDiscoveryProperties.getNamespace());
            beatInfo.setServiceName(nacosDiscoveryProperties.getService());
            beatInfo.setClusterName(nacosDiscoveryProperties.getClusterName());
            beatInfo.setIp(nacosDiscoveryProperties.getClientIp());
            beatInfo.setPort(nacosDiscoveryProperties.getClientPort());
            if(state==IdleState.WRITER_IDLE){//写空闲
                //发送心跳...
                //message加上协议字符来区分消息
                MessageProtocol messageProtocol=new MessageProtocol();
                //message加上协议字符来区分消息
                String message= Constants.BEAT_ROUND+JSON.toJSONString(beatInfo)+Constants.BEAT_ROUND;
                messageProtocol.setLen(message.getBytes().length);
                messageProtocol.setContent(message.getBytes());
                ctx.writeAndFlush(messageProtocol);
                log.info("发送心跳...");
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        //传给下一个hannler处理
        ctx.fireChannelRead(msg);
    }
}
