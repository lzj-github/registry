package cn.lzj.nacos.naming.controller;

import cn.lzj.nacos.naming.cluster.ServerListManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @Autowired
    private ServerListManager serverListManager;

    @RequestMapping("/server/status")
    public String serverStatus(@RequestParam String serverStatus) {
        serverListManager.onReceiveServerStatus(serverStatus);
        return "ok";
    }

}
