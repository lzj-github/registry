package cn.lzj.nacos.naming.netty.handler;


import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.BeatInfo;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.naming.core.ServiceManager;
import cn.lzj.nacos.naming.netty.AcceptorIdleStateTrigger;
import cn.lzj.nacos.naming.netty.MessageProtocol;
import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Data
@Slf4j
@Component
public class ServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {

    @Autowired
    private  ServiceManager serviceManager;

    public ServerHandler(ServiceManager serviceManager) {
        this.serviceManager=serviceManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
        String msgStr=new String(msg.getContent());
        log.info("有消息来了。。。。。。。。。。。。。。。。。。。");
        if(msgStr.startsWith(Constants.BEAT_ROUND)&& msgStr.endsWith(Constants.BEAT_ROUND)){
            log.info("=====服务端接收到心跳消息如下======");
            BeatInfo beatInfo = JSON.parseObject(getRealMsg(msgStr), BeatInfo.class);
            log.info(beatInfo.toString());
            String key=beatInfo.getNamespaceId()+"##"+beatInfo.getServiceName();
            if(!AcceptorIdleStateTrigger.dataMap.containsKey(ctx.channel().remoteAddress())){
                //接收到某个实例的第一次消息(包括注册或心跳)，就把该实例的socketAddress与它的namespaceId和serviceName存起来，方便后续在内存注册表剔除该实例
                AcceptorIdleStateTrigger.dataMap.put(ctx.channel().remoteAddress(),beatInfo.getNamespaceId()+"##"+beatInfo.getServiceName()+"##"+beatInfo.getIp()+"##"+beatInfo.getPort());
            }
            System.out.println(AcceptorIdleStateTrigger.dataMap);
            //收到心跳就把读空闲次数重新设置为0
            AcceptorIdleStateTrigger.readIdleTimesMap.put(ctx.channel().remoteAddress(),0);
        }else if(msgStr.startsWith(Constants.REGISTER_SERVICE_ROUND)&&msgStr.endsWith(Constants.REGISTER_SERVICE_ROUND)){
            log.info("=====服务端接收到注册消息如下======");
            Instance instance = JSON.parseObject(getRealMsg(msgStr),Instance.class);
            log.info(instance.toString());
            if(!AcceptorIdleStateTrigger.dataMap.containsKey(ctx.channel().remoteAddress())){
                System.out.println("注册");
                //接收到某个实例的第一次消息(包括注册或心跳)，就把该实例的socketAddress与它的namespaceId和serviceName存起来，方便后续在内存注册表剔除该实例
                AcceptorIdleStateTrigger.dataMap.put(ctx.channel().remoteAddress(),instance.getNamespaceId()+"##"+instance.getServiceName()+"##"+instance.getIp()+"##"+instance.getPort());
            }
            serviceManager.registerInstance(instance);
        }

    }

    //实例上线
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //初始化连接的时候读空闲次数为0
        log.info(ctx.channel().remoteAddress()+":上线了");
        AcceptorIdleStateTrigger.readIdleTimesMap.put(ctx.channel().remoteAddress(),0);
    }

    //实例下线
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error(ctx.channel().remoteAddress()+":断线了......");
        deleteIPs(ctx);
        super.channelInactive(ctx);
    }

    public void deleteIPs(ChannelHandlerContext ctx){
        if(AcceptorIdleStateTrigger.dataMap.get(ctx.channel().remoteAddress())!=null){
            String str[]=AcceptorIdleStateTrigger.dataMap.get(ctx.channel().remoteAddress()).split("##");
            String namespaceId=str[0];
            String serviceName=str[1];
            String ip=str[2];
            String port=str[3];
            Instance deleteInstance=new Instance();
            deleteInstance.setNamespaceId(namespaceId);
            deleteInstance.setServiceName(serviceName);
            deleteInstance.setIp(ip);
            deleteInstance.setPort(Integer.valueOf(port));
            AcceptorIdleStateTrigger.readIdleTimesMap.remove(ctx.channel().remoteAddress());
            AcceptorIdleStateTrigger.dataMap.remove(ctx.channel().remoteAddress());
            serviceManager.removeInstance(namespaceId,serviceName,deleteInstance);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //发生异常,关闭链路
        ctx.close();
    }


    //去除协议字符
    private String getRealMsg(String msg){
        return msg.substring(Constants.PROTOCOL_LEN,msg.length()-Constants.PROTOCOL_LEN);
    }
}
