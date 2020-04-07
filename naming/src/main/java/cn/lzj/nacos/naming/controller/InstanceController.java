package cn.lzj.nacos.naming.controller;

import cn.lzj.nacos.naming.netty.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/instance")
public class InstanceController {

    @Autowired
    private NettyServer nettyServer;

    @GetMapping
    public String test(){
        nettyServer.start();
        return "ok";
    }

    @PostMapping
    public String register(HttpServletRequest request) throws Exception {

        String serviceName = null;
        String namespaceId = null;


      //  serviceManager.registerInstance(namespaceId, serviceName, parseInstance(request));
        return "ok";
    }

}
