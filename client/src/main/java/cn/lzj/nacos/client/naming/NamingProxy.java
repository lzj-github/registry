package cn.lzj.nacos.client.naming;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.BeatInfo;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.client.config.DiscoveryProperties;
import cn.lzj.nacos.client.netty.MessageProtocol;
import cn.lzj.nacos.client.netty.NettyClient;
import com.alibaba.fastjson.JSON;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class NamingProxy {

    @Autowired
    private DiscoveryProperties discoveryProperties;

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
        log.info("实例:{} 注册服务:{}", instance, serviceName);
    }

    /**
     * 发送获取服务列表的请求
     * @param namespaceId
     * @return
     */
    public void queryList(String namespaceId){
        Channel channel=NettyClient.channel;
        //int index=random.nextInt(clusters.size());
        //String server=clusters.get(index);
        MessageProtocol messageProtocol=new MessageProtocol();
        String message=Constants.SERVICE_FOUND_ROUND+namespaceId+Constants.SERVICE_FOUND_ROUND;
        messageProtocol.setLen(message.getBytes().length);
        messageProtocol.setContent(message.getBytes());
        channel.writeAndFlush(messageProtocol);
        log.info("客户端服务发现请求");

    }
}
