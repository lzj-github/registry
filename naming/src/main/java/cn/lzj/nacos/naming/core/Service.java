package cn.lzj.nacos.naming.core;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.naming.boot.SpringContext;
import cn.lzj.nacos.naming.consistency.RecordListener;
import cn.lzj.nacos.naming.push.PushService;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Data
public class Service implements RecordListener {

    private String namespaceId;

    private String Name;

    private String groupName;

    //Map<namespaceId##serviceName,List<Instance>>
    private Map<String, List<Instance>> clusterMap=new ConcurrentHashMap<>();


    public List<Instance> allIPs(String serviceName) {
        return clusterMap.get(serviceName);
    }


    public void init() {
        log.info("Service实例的初始化方法被调用了");
    }

    /**
     * 监听注册的实现方法
     * @param key
     * @param value
     */
    @Override
    public void onChange(String key, Instances value) {
        log.info("有事件触发了，观察者的onChange方法被调用了  key="+key+",value="+value);
        updateIPs(value.getInstanceList());
    }

    public void updateIPs(List<Instance> instanceList) {
        Map<String,List<Instance>> ipMap=new HashMap<>(clusterMap.size());
        for(String clusterName:clusterMap.keySet()){
            ipMap.put(clusterName,new ArrayList<>());
        }
        for(Instance instance:instanceList){
            String clusterName=instance.getNamespaceId() + "##" + instance.getServiceName();
            if(instance==null){
                continue;
            }
            List<Instance> clusterIPs = ipMap.get(clusterName);
            //第一次创建该服务
            if(clusterIPs==null){
                clusterIPs=new LinkedList<>();
                ipMap.put(clusterName,clusterIPs);
            }
            clusterIPs.add(instance);
        }
        //把ipMap最终赋值给service里的clusterMap
        clusterMap=ipMap;
        log.info("service的clusteMap:"+clusterMap);
        //最后再通知serviceManager更新内存注册表,因为service对象不是交给spring容器管理，
        // 所以不能直接使用@Autowired serviceManager，不然拿到的为空
        //通过另一个由spring容器管理的类来发布事件
        getPushServcie().serviceChanged(this);
    }

    public PushService getPushServcie() {
        return SpringContext.getAppContext().getBean(PushService.class);
    }

    @Override
    public void onDelete(String key) {

    }
}
