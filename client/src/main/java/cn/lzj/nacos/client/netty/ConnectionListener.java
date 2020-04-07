package cn.lzj.nacos.client.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 启动客户端的时候连接不上服务器，开始重连
 */
@Slf4j
public class ConnectionListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        boolean successd=future.isSuccess();
        //如果重连失败，则调用ChannelInactive方法，再次出发重连事件，一直尝试12次，如果失败则不再重连
        if (!successd) {
            log.error("重连失败...");
            future.channel().pipeline().fireChannelInactive();
        }else{
            log.info("重连服务器成功...");
        }
    }
}
