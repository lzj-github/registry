package cn.lzj.nacos.naming.cluster;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.naming.config.NetConfig;
import cn.lzj.nacos.naming.misc.GlobalExecutor;
import cn.lzj.nacos.naming.misc.Message;
import cn.lzj.nacos.naming.misc.ServerStatusSynchronizer;
import cn.lzj.nacos.naming.misc.Synchronizer;
import cn.lzj.nacos.naming.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component("serverListManager")
public class ServerListManager {

    //所有的server节点，包括健康的和非健康的
    private List<Server> servers = new ArrayList<>();

    private List<ServerChangeListener> listeners = new ArrayList<>();

    //可用的server节点
    private List<Server> healthyServers = new ArrayList<>();

    private Synchronizer synchronizer = new ServerStatusSynchronizer();

    //Map<serverIp:serverPort,timestamps>
    private Map<String, Long> distroBeats = new ConcurrentHashMap<>(16);

    @Autowired
    private NetConfig netConfig;

    @PostConstruct
    public void init() {
        GlobalExecutor.registerServerListUpdater(new ServerListUpdater());
        GlobalExecutor.registerServerStatusReporter(new ServerStatusReporter(), 5);
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

    /**
     * 别的server发送状态后调用该方法，加锁是因为是异步发送http请求的
     * @param serverStatus
     */
    public synchronized void onReceiveServerStatus(String serverStatus) {

        if(serverStatus.length()==0){
            return;
        }

        List<Server> newHealthyList = new ArrayList<>();
        List<Server> tempServerList=new ArrayList<>();

        //cluster_status#192.168.153.1:9002#1586336129841#
        String[] params=serverStatus.split("#");
        Server server=new Server();
        server.setIp(params[1].split(Constants.IP_PORT_SPLITER)[0]);
        server.setServePort(Integer.parseInt(params[1].split(Constants.IP_PORT_SPLITER)[1]));
        server.setLastRefTime(Long.parseLong(params[2]));

        Long lastBeat=distroBeats.get(server.getKey());
        long now = System.currentTimeMillis();
        if(null!=lastBeat){
            //不是第一次发送心跳,太久才发一次心跳(15s)的话也把该server节点设为非存活状态，等下次再发送一次心跳间隔小于15s才设置为存活状态
            server.setAlive(now-lastBeat<Constants.SEVER_EXPIRED_MILLS);
        }
        distroBeats.put(server.getKey(),now);

        Date date=new Date(Long.parseLong(params[2]));
        //格式化时间戳
        server.setLastRefTimeStr(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));







    }

    private class ServerStatusReporter implements Runnable {

        @Override
        public void run() {
            try{

                //自己当前的ip加端口
                String serverAddr=netConfig.getServerIp()+ Constants.IP_PORT_SPLITER+netConfig.getServerPort();
                String status="cluster_status#"+serverAddr+"#"+System.currentTimeMillis()+"#";

                //发送心跳给自己
                onReceiveServerStatus(status);

                List<Server> allServers=servers;

                if(allServers.size()>0){
                    for(Server server:allServers){
                        if(server.getKey().equals(serverAddr)){
                            //跳过自己本身
                            continue;
                        }
                        Message message=new Message();
                        message.setData(status);
                        //给别的server发送自己的状态
                        synchronizer.send(server.getKey(), message);

                    }
                }
            }catch (Exception e) {
                log.error("发送server状态的过程中出现了错误:", e);
            } finally {
                //3s后再执行一次
                GlobalExecutor.registerServerStatusReporter(this, Constants.SERVER_STATUS_SYNCHRONIZATION_PERIOD_MILLIS);
            }
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
