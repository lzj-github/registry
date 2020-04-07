package cn.lzj.nacos.naming.core;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.naming.netty.NettyServer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StartUp implements InitializingBean {

    @Autowired
    NettyServer nettyServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        nettyServer.start();
    }
}
