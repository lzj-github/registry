package cn.lzj.nacos.naming.netty;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.naming.core.ServiceManager;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ChannelHandler.Sharable
@Slf4j
@Component
public class AcceptorIdleStateTrigger extends SimpleChannelInboundHandler<MessageProtocol> {

    //Map<SocketAddress,Integer readIdleTimes>
    public static Map<SocketAddress,Integer> readIdleTimesMap=new ConcurrentHashMap<>();

    //Map<SocketAddress,String namesapce##serviceName##ip##port>
    public static Map<SocketAddress,String> dataMap=new ConcurrentHashMap<>();

    /**
     * 服务端要对心跳包做出响应，其实给客户端最好的回复就是“不回复”，这样可以服务端的压力，
     * 假如有10w个空闲Idle的连接，那么服务端光发送心跳回复，则也是费事的事情，那么怎么才能告诉客户端它还活着呢，
     * 其实很简单，因为5s服务端都会收到来自客户端的心跳信息，那么如果15秒内收不到，服务端可以认为客户端挂了，可以close链路
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        log.info(socketAddress+":读空闲了");
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                Integer readIdleTimes = readIdleTimesMap.get(socketAddress);
                System.out.println("readIdleTimes:" + readIdleTimes);
                readIdleTimes++;
                readIdleTimesMap.put(socketAddress, readIdleTimes);
                if (readIdleTimes > 3) {
                    log.error(socketAddress + "读空闲超过3次，关闭连接，释放更多资源");
                    MessageProtocol messageProtocol = new MessageProtocol();
                    //message加上协议字符来区分消息
                    String message = Constants.DISCONNECT_ROUND + "idle close" + Constants.DISCONNECT_ROUND;
                    messageProtocol.setLen(message.getBytes().length);
                    messageProtocol.setContent(message.getBytes());
                    ctx.writeAndFlush(messageProtocol);
                    ctx.channel().close();
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        ctx.fireChannelRead(msg);
    }
}
