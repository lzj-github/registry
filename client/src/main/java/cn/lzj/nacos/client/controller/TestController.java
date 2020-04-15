package cn.lzj.nacos.client.controller;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.client.discovery.RegistryDiscoveryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    RegistryDiscoveryClient registryDiscoveryClient;

    @RequestMapping("/test")
    public String test(){
        List<Instance> abc = registryDiscoveryClient.getInstances("abc");
        String targetUri=abc.get(0).getUri().toString();//  http://192.168.153.1:8080
        ResponseEntity<String> forEntity = restTemplate.getForEntity(targetUri + "/test2", String.class);
        System.out.println(forEntity.getBody());
        return "ok";
    }

    @RequestMapping("/test2")
    public String test2(){
        return "ok";
    }
}
