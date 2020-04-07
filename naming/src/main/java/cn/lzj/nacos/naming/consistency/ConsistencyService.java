package cn.lzj.nacos.naming.consistency;

import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.core.Service;

public interface ConsistencyService {


    void put(String key, Instances instances);

    void remove(String key);

    void listen(String key, RecordListener listener);
}
