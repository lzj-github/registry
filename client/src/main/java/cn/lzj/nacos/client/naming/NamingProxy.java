package cn.lzj.nacos.client.naming;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.BeatInfo;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.client.netty.MessageProtocol;
import cn.lzj.nacos.client.netty.NettyClient;
import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NamingProxy {
//    @Autowired
//    private NettyClient nettyClient;

    /*public void sendBeat(BeatInfo beatInfo) {
        Channel channel = nettyClient.getChannel();
        MessageProtocol messageProtocol=new MessageProtocol();
        //message加上协议字符来区分消息
        String message= Constants.BEAT_ROUND+JSON.toJSONString(beatInfo)+Constants.BEAT_ROUND;
        messageProtocol.setLen(message.getBytes().length);
        messageProtocol.setContent(message.getBytes());
        channel.writeAndFlush(messageProtocol);
    }
*/

    /**
     * 发送注册信息
     * @param serviceName
     * @param instance
     */
    public void registerService(String serviceName, Instance instance) {
        Channel channel=NettyClient.channel;
        MessageProtocol messageProtocol=new MessageProtocol();
        String message=Constants.REGISTER_SERVICE_ROUND+JSON.toJSONString(instance) +Constants.REGISTER_SERVICE_ROUND;
        messageProtocol.setLen(message.getBytes().length);
        messageProtocol.setContent(message.getBytes());
        channel.writeAndFlush(messageProtocol);
        log.info("registering service {} with instance: {}",  serviceName, instance);
    }
}
