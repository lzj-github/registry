package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoundRobinRule implements IRule {
    @Override
    public Instance choose(List<Instance> instances) {
        return null;
    }
}
