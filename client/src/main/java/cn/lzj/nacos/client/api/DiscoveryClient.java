package cn.lzj.nacos.client.api;

import cn.lzj.nacos.api.pojo.Instance;

import java.util.List;

public interface DiscoveryClient {

    List<Instance> getInstances(String serviceId);
}
