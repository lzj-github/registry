package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BaseLoadBalancer implements ILoadBalancer {

    @Autowired
    private IRule iRule;

    @Override
    public Instance chooseInstance(List<Instance> instances) {
        return iRule.choose(instances);
    }
}
