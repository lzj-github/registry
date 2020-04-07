package cn.lzj.nacos.client.controller;

import cn.lzj.nacos.client.config.NacosDiscoveryProperties;
import cn.lzj.nacos.client.naming.NacosNamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class TestController {

    @Autowired
    NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    NacosNamingService  nacosNamingService;

    @GetMapping("/index")
    public String register() throws Exception {
        nacosNamingService.afterPropertiesSet();
        return "ok";
    }
}
