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
                if(!success){
                    SyncTask syncTask1=new SyncTask();
                    syncTask1.setDataMap(syncTask.getDataMap());
                    syncTask1.setTargetServer(syncTask.getTargetServer());
                    retrySync(syncTask1,delay);
                }
            }
        },delay);
    }

    /**
     * 失败重发
     * @param syncTask
     */
    public void retrySync(SyncTask syncTask,long delay) {
        Server server = new Server();
        server.setIp(syncTask.getTargetServer().split(":")[0]);
        server.setServePort(Integer.parseInt(syncTask.getTargetServer().split(":")[1]));
        if (!getServers().contains(server)) {
            // 如果服务器不在健康服务器列表中，就忽略这次同步任务
            return;
        }

        submit(syncTask,delay);
    }
}
