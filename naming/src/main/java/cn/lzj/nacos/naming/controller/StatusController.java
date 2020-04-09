package cn.lzj.nacos.naming.controller;

import cn.lzj.nacos.naming.cluster.ServerListManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class StatusController {

    @Autowired
    private ServerListManager serverListManager;

    @RequestMapping("/server/status")
    public String serverStatus(@RequestParam String serverStatus) {
        log.info("收到集群间的心跳:"+serverStatus);
        serverListManager.onReceiveServerStatus(serverStatus);
        return "ok";
    }

}
