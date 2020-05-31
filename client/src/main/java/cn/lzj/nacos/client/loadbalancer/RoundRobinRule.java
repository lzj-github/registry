package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRule implements IRule {

    private AtomicInteger nextCounter= new AtomicInteger(0);

    /**
     * 轮询选择实例
     * @param instances
     * @return
     */
    @Override
    public Instance choose(List<Instance> instances) {
        for(;;){
            int current=nextCounter.get();
            int next=(current+1)%instances.size();
            //CAS有3个操作数，内存值V，旧的预期值A，要修改的新值B。
            // 当且仅当预期值A和内存值V相同时，将内存值V修改为B，否则什么都不做。
            if(nextCounter.compareAndSet(current,next)){
                return instances.get(next);
            }
        }
    }
}
