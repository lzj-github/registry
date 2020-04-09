package cn.lzj.nacos.naming.consistency;

import cn.lzj.nacos.naming.cluster.Server;
import cn.lzj.nacos.naming.cluster.ServerListManager;
import cn.lzj.nacos.naming.core.Instances;
import cn.lzj.nacos.naming.misc.GlobalExecutor;
import cn.lzj.nacos.naming.misc.ServerSynchronizer;
import cn.lzj.nacos.naming.misc.Synchronizer;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import org.redisson.Redisson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@DependsOn("serverListManager")
public class DataSyncer {

    @Autowired
    private ServerListManager serverListManager;

    @Autowired
    private Redisson redisson;

    private Synchronizer synchronizer = new ServerSynchronizer();

    public List<Server> getServers() {
        return serverListManager.getHealthyServers();
    }

    public void submit(SyncTask syncTask, long delay) {

        GlobalExecutor.submitDataSync(new Runnable() {
            @Override
            public void run() {
                byte[] data=JSON.toJSONBytes(syncTask.getDataMap());
                //开始发送同步信息
                boolean success = synchronizer.syncData(syncTask.getTargetServer(), data);
            }
        },delay);
    }
}
