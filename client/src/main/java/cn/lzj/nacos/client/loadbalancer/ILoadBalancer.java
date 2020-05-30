package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;

import java.util.List;

public interface ILoadBalancer {

    public Instance chooseInstance(List<Instance> instances);
}
