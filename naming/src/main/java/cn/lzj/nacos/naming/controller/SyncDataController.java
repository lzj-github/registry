package cn.lzj.nacos.naming.controller;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.naming.consistency.ConsistencyServiceImpl;
import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.core.Service;
import cn.lzj.nacos.naming.core.ServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class SyncDataController {

    @Autowired
    private ServiceManager serviceManager;

    @Autowired
    private ConsistencyServiceImpl consistencyService;

    @PutMapping("/data/sync")
    public ResponseEntity<String> onSyncData(@RequestBody Map<String, Instances> dataMap) throws Exception {

        if (dataMap.isEmpty()) {
            log.error("集群同步: 接收的实例为空!");
        }

        log.info("集群数据同步成功，"+dataMap);
        for (Map.Entry<String, Instances> entry : dataMap.entrySet()) {
            String namespaceId = entry.getKey().split("##")[0];
            String serviceName = entry.getKey().split("##")[1];
            if (serviceManager.getService(namespaceId, serviceName) == null) {
                serviceManager.createServiceIfAbsent(namespaceId, serviceName);
            }
            Instances instances = entry.getValue();
            //别的集群同步过来的实例列表
            List<Instance> otherInstanceList = instances.getInstanceList();
            Service service=serviceManager.getService(namespaceId,serviceName);
            //拿出该服务存的全部实例
            List<Instance> instanceList=service.allIPs(namespaceId+"##"+serviceName);
            if(instanceList==null){
                instanceList=new ArrayList<>();
            }
            List<Instance> newInstanceList=null;
            //如果是新增实例的情况下
            if(otherInstanceList.size()>=instanceList.size()){
                //别人同步过来的和自己的实例列表做并集，不是像之前那样直接覆盖，避免数据覆盖而导致数据丢失
                newInstanceList= (List<Instance>) CollectionUtils.union(otherInstanceList,instanceList);
            }else if(otherInstanceList.size()<instanceList.size()){//删除节点的同步
                //通过差集来赋值
                newInstanceList= (List<Instance>) CollectionUtils.subtract(instanceList,(List<Instance>)CollectionUtils.subtract(instanceList,otherInstanceList));
            }

            Instances newInstances=new Instances();
            newInstances.setInstanceList(newInstanceList);
            consistencyService.onPut(entry.getKey(), newInstances, UUID.randomUUID().toString());

        }
        return ResponseEntity.ok("ok");
    }

}
