package cn.lzj.nacos.naming.cluster;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.naming.core.GlobalExecutor;
import cn.lzj.nacos.naming.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("serverListManager")
public class ServerListManager {

    //所有的server节点，包括健康的和非健康的
    private List<Server> servers = new ArrayList<>();

    private List<ServerChangeListener> listeners = new ArrayList<>();

    //可用的server节点
    private List<Server> healthyServers = new ArrayList<>();

    @PostConstruct
    public void init() {
        GlobalExecutor.registerServerListUpdater(new ServerListUpdater());
        GlobalExecutor.registerServerStatusReporter(new ServerStatusReporter(), 30);
    }

    /**
     * 添加观察者
     * @param listener
     */
    public void listen(ServerChangeListener listener) {
        listeners.add(listener);
    }


    /**
     * 返回配置文件读取的server列表
     * @return
     */
    private List<Server> refreshServerList() {
        List<Server> result = new ArrayList<>();
        List<String> serverList=new ArrayList<>();
        try {
            serverList = SystemUtils.readClusterConf();
            log.info("The server IP list of Nacos is {}", serverList);
        } catch (IOException e) {
            log.error("read cluster conf fail", e);
        }
        if(CollectionUtils.isNotEmpty(serverList)){
            for(int i=0;i<serverList.size();i++){
                String ip;
                int port;
                String server=serverList.get(i);
                ip=server.split(Constants.IP_PORT_SPLITER)[0];
                port= Integer.parseInt(server.split(Constants.IP_PORT_SPLITER)[1]);

                Server member=new Server();
                member.setIp(ip);
                member.setServePort(port);
                result.add(member);
            }
        }
        return result;
    }

    public class ServerListUpdater implements Runnable{

        @Override
        public void run() {
            try {
                List<Server> refreshedServers = refreshServerList();
                List<Server> oldServers = servers;

                boolean changed=false;

                List<Server> newServers = (List<Server>) CollectionUtils.subtract(refreshedServers, oldServers);
                if (CollectionUtils.isNotEmpty(newServers)) {
                    //改变了配置文件log，增加了新的集群节点
                    servers.addAll(newServers);
                    changed = true;
                    log.info("server list is updated, new: {} servers: {}", newServers.size(), newServers);
                }

                List<Server> deadServers = (List<Server>) CollectionUtils.subtract(oldServers, refreshedServers);
                if (CollectionUtils.isNotEmpty(deadServers)) {
                    //删除了旧的集群节点
                    servers.removeAll(deadServers);
                    changed = true;
                    log.info("server list is updated, dead: {}, servers: {}", deadServers.size(), deadServers);
                }

                if (changed) {
                    notifyListeners();
                }

            }catch (Exception e){
                log.error("error while updating server list.", e);
            }
        }

    }

    private class ServerStatusReporter implements Runnable {

        @Override
        public void run() {

        }
    }

    private void notifyListeners() {
        GlobalExecutor.notifyServerListChange(new Runnable() {
            @Override
            public void run() {
                //通知其他节点集群列表更改了
                for (ServerChangeListener listener : listeners) {
                    listener.onChangeServerList(servers);
                    listener.onChangeHealthyServerList(healthyServers);
                }
            }
        });
    }
}
