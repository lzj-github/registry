package cn.lzj.nacos.client.discovery;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.client.api.DiscoveryClient;
import cn.lzj.nacos.client.api.NamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistryDiscoveryClient implements DiscoveryClient {

    @Autowired
    NamingService namingService;

    @Override
    public List<Instance> getInstances(String serviceName) {
        return namingService.selectInstances(serviceName);
    }
}
