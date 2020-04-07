package cn.lzj.nacos.naming.core;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.naming.consistency.ConsistencyService;
import cn.lzj.nacos.naming.push.ServiceChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ServiceManager implements ApplicationListener<ServiceChangeEvent> {

    @Autowired
    ConsistencyService consistencyService;

    //Map<namespaceId,Map<serviceName,Service>>  内存注册表
    private Map<String, Map<String, Service>> serviceMap=new ConcurrentHashMap<>();

    /**
     * 注册实例
     * @param instance
     */
    public void registerInstance(Instance instance) {
        String namespaceId=instance.getNamespaceId();
        String serviceName=instance.getServiceName();
        //先尝试创建新的服务
        createServiceIfAbsent(namespaceId,serviceName);
        //拿到当前服务
        Service service=getService(namespaceId,serviceName);
        log.info("service:"+service);
        if(service==null){
            throw new RuntimeException("service not found, namespace: " + namespaceId + ", service: " + serviceName);
        }

        //添加实例到该服务里面去
        addInstance(namespaceId, serviceName,  instance);

    }

    /**
     * 添加实例到服务的实例列表，以及放到内存注册表里去
     * @param namespaceId
     * @param serviceName
     * @param instance
     */
    public void addInstance(String namespaceId, String serviceName, Instance instance) {
        Service service=getService(namespaceId,serviceName);
        //拿出该服务存的全部实例
        List<Instance> instanceList=service.allIPs(namespaceId+"##"+serviceName);
        if(instanceList==null){
            instanceList=new ArrayList<>();
        }
        instanceList.add(instance);
        //不能在这里就把实例加到service里的list里面，因为防止同时多个来注册，会覆盖之前的
        //添加新的实例到服务里的实例list里面
        //service.getClusterMap().put(serviceName,instances);
        Instances instances=new Instances();
        instances.setInstanceList(instanceList);
        String key=namespaceId+"##"+serviceName;
        consistencyService.put(key,instances);
    }

    /**
     * 删除实例
     * @param namespaceId
     * @param serviceName
     * @param instance
     */
    public void removeInstance(String namespaceId, String serviceName, Instance instance){
        Service service=getService(namespaceId,serviceName);
        //拿出该服务存的全部实例
        List<Instance> instanceList=service.allIPs(namespaceId+"##"+serviceName);
        //把该实例从实例列表中移除
        instanceList.remove(instance);
        Instances instances=new Instances();
        instances.setInstanceList(instanceList);
        String key=namespaceId+"##"+serviceName;
        consistencyService.put(key,instances);
    }

    /**
     * 创建新的服务
     * @param namespaceId
     * @param serviceName
     */
    public void createServiceIfAbsent(String namespaceId, String serviceName) {
        Service service=getService(namespaceId,serviceName);
        //该服务如果为空，即这是该服务第一实例，则创建该服务
        if(service==null){
            service=new Service();
            service.setNamespaceId(namespaceId);
            service.setName(serviceName);
            log.info("第一次创建该服务:"+service);
            //把service放在缓存map中
            putService(service);
            //调用Service的init方法，init方法里面做健康检查
            service.init();
            //注册观察者
            consistencyService.listen(namespaceId+"##"+serviceName,service);
        }
    }

    public void putService(Service service){
        if(!serviceMap.containsKey(service.getNamespaceId())){
            //放第一层map  map<namespaceId,Map<>>
            serviceMap.put(service.getNamespaceId(),new ConcurrentHashMap<>(16));
        }
        //放第二层map  map<serviceName,service>
        serviceMap.get(service.getNamespaceId()).put(service.getName(),service);
    }


    /**
     * 通过namespaceId和serviceName取出service
     * @param namespaceId
     * @param serviceName
     * @return
     */
    public Service getService(String namespaceId, String serviceName) {
        if(serviceMap.get(namespaceId)==null){
            return null;
        }
        return serviceMap.get(namespaceId).get(serviceName);
    }

    @Override
    public void onApplicationEvent(ServiceChangeEvent serviceChangeEvent) {
        Service service = serviceChangeEvent.getService();
        //放进内存注册表
        putService(service);
        log.info("服务注册完成，内存注册表:"+serviceMap);
    }
}
