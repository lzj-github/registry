package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomRule implements IRule {

    /**
     * 随机选择实例
     * @param instances
     * @return
     */
    @Override
    public Instance choose(List<Instance> instances) {
        int index = (int) (Math.random()*instances.size());
        return instances.get(index);
    }
}
