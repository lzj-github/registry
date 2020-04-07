package cn.lzj.nacos.client.core;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.ServiceInfo;
import cn.lzj.nacos.client.naming.NamingProxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class HostReactor {

    @Autowired
    private NamingProxy namingProxy;

    //客户端实例缓存map Map<serviceName,ServiceInfo>
    private Map<String, ServiceInfo> serviceInfoMap=new ConcurrentHashMap<>();

    private ScheduledExecutorService executor;

    public void getServiceInfo(String namespaceId)  {

        //先尝试从客户端缓存map中获取实例列表
        Map<String,ServiceInfo> services=serviceInfoMap;

        //第一次缓存map为空
        if(services.size()==0){
            //向服务端发送请求
            updateServiceNow(namespaceId);
        }
        //定时拉取服务器上的实例注册表
        scheduleUpdate(namespaceId);

    }

    public void updateServiceNow(String namespaceId){
        namingProxy.queryList(namespaceId);
    }

    private void scheduleUpdate(String namespaceId) {
        executor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("client.naming.updater");
                return thread;
            }
        });
        //每15s拉取一次服务端的注册数据
        executor.scheduleWithFixedDelay(new UpdateTask(namespaceId),0, Constants.SERVICE_FOUND_REFRESH_INTEEVAL, TimeUnit.SECONDS);
    }

    public class UpdateTask implements Runnable {

        private String namespaceId;

        public UpdateTask(String namespaceId) {
            this.namespaceId=namespaceId;
        }

        @Override
        public void run() {
            namingProxy.queryList(namespaceId);
        }
    }

    //返回客户端的缓存map
    public Map<String,ServiceInfo> getAllInstances(){
        return serviceInfoMap;
    }

    //收到服务端传来的实例信息，设置到客户端的缓存中
    public void putService(Map<String, ServiceInfo> services) {
        serviceInfoMap=services;
        log.info("更新客户端缓存成功,缓存实例map:"+serviceInfoMap);
    }
}
