package cn.lzj.nacos.client.naming;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.BeatInfo;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.client.api.NamingService;
import cn.lzj.nacos.client.config.NacosDiscoveryProperties;
import cn.lzj.nacos.client.netty.NettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class NacosNamingService implements NamingService, InitializingBean {

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    private NettyClient nettyClient;

    @Autowired
    private NamingProxy namingProxy;

    /**
     *注册实例
     */
    @Override
    public void registerInstance(String serviceName, String groupName, String ip, int port,String nameSpaceId) {
        Instance instance=new Instance();
        instance.setNamespaceId(nameSpaceId);
        instance.setServiceName(serviceName);
        instance.setIp(ip);
        instance.setPort(port);
        instance.setClusterName(groupName);
        namingProxy.registerService(serviceName,instance);
    }


    @Override
    public void afterPropertiesSet() throws Exception {

        CountDownLatch countDownLatch=new CountDownLatch(1);
        class Task implements Runnable{

            private CountDownLatch countDownLatch;
            public Task(CountDownLatch countDownLatch){
                this.countDownLatch=countDownLatch;
            }

            @Override
            public void run() {
                try {
                    //等待netty启动好了才注册实例
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //注册服务
                registerInstance(nacosDiscoveryProperties.getService(),nacosDiscoveryProperties.getClusterName(),
                        nacosDiscoveryProperties.getClientIp(),nacosDiscoveryProperties.getClientPort(),nacosDiscoveryProperties.getNamespace());

            }
        }
        new Thread(new Task(countDownLatch)).start();
        //启动netty服务
        nettyClient.start(countDownLatch);
    }
}
