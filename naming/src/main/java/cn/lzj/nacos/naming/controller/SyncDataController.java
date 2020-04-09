package cn.lzj.nacos.naming.controller;

import cn.lzj.nacos.naming.consistency.ConsistencyServiceImpl;
import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.core.ServiceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.util.Loggers;

import java.util.Map;

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
            consistencyService.onPut(entry.getKey(), entry.getValue());

        }
        return ResponseEntity.ok("ok");
    }

}
