package cn.lzj.nacos.client.naming;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.api.pojo.ServiceInfo;
import cn.lzj.nacos.client.api.NamingService;
import cn.lzj.nacos.client.config.DiscoveryProperties;
import cn.lzj.nacos.client.core.HostReactor;
import cn.lzj.nacos.client.netty.NettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class NacosNamingService implements NamingService, InitializingBean {

    @Autowired
    private DiscoveryProperties discoveryProperties;

    @Autowired
    private NettyClient nettyClient;

    @Autowired
    private NamingProxy namingProxy;

    @Autowired
    private HostReactor hostReactor;

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

    /**
     * 服务发现
     * @param namespaceId
     * @return
     */
    @Override
    public void serviceFound(String namespaceId){
        hostReactor.getServiceInfo(namespaceId);
    }

    /**
     * 返回所有实例
     * @return
     */
    public Map<String, ServiceInfo> getAllInstance(){
        return hostReactor.getAllInstances();
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
                registerInstance(discoveryProperties.getService(), discoveryProperties.getClusterName(),
                        discoveryProperties.getClientIp(), discoveryProperties.getClientPort(), discoveryProperties.getNamespace());
                //从服务端那里获取实例列表
                serviceFound( discoveryProperties.getNamespace());

            }
        }
        new Thread(new Task(countDownLatch)).start();
        //启动netty服务
        nettyClient.start(countDownLatch);
    }
}
