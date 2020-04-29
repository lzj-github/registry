package cn.lzj.nacos.naming.consistency;

import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.core.Service;

import java.util.Map;

public interface ConsistencyService {


    void put(String key, Instances instances,String messageId);

    void remove(String key);

    void listen(String key, RecordListener listener);

    public void setInstance(String key,Instances instances);

     Map<String, Instances> getInstances();

    void notifyCluster(String key);
}
