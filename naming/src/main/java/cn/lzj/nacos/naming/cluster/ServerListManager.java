package cn.lzj.nacos.naming.cluster;

import cn.lzj.nacos.api.common.Constants;
import cn.lzj.nacos.naming.config.NetConfig;
import cn.lzj.nacos.naming.consistency.ConsistencyService;
import cn.lzj.nacos.naming.misc.GlobalExecutor;
import cn.lzj.nacos.naming.misc.Message;
import cn.lzj.nacos.naming.misc.ServerSynchronizer;
import cn.lzj.nacos.naming.misc.Synchronizer;
import cn.lzj.nacos.naming.netty.NettyServer;
import cn.lzj.nacos.naming.netty.handler.ServerHandler;
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

    private Synchronizer synchronizer = new ServerSynchronizer();

    //Map<serverIp:serverPort,timestamps>
    private Map<String, Long> distroBeats = new ConcurrentHashMap<>(16);

    //为了保存着最新的server更新心跳时间后的数据
    private Map<String, List<Server>> distroConfig = new ConcurrentHashMap<>();

    //心跳消息前缀
    private final static String LOCALHOST_SITE="cluster_status";

    @Autowired
    private NetConfig netConfig;

    @Autowired
    private ConsistencyService consistencyService;



    //判断是否刚启动或者刚重启，第一次发送心跳
    private static boolean isFisrtSendHeartBeat=true;

    @PostConstruct
    public void init() {
        GlobalExecutor.registerServerListUpdater(new ServerListUpdater());
        GlobalExecutor.registerServerStatusReporter(new ServerStatusReporter(), 1);
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
            log.info("server列表的ip是: {}", serverList);
        } catch (IOException e) {
            log.error("读取集群配置文件失败", e);
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
                    log.info("server列表更新了, new: {} servers: {}", newServers.size(), newServers);
                }

                List<Server> deadServers = (List<Server>) CollectionUtils.subtract(oldServers, refreshedServers);
                if (CollectionUtils.isNotEmpty(deadServers)) {
                    //删除了旧的集群节点
                    servers.removeAll(deadServers);
                    changed = true;
                    log.info("server列表更新了, dead: {}, servers: {}", deadServers.size(), deadServers);
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
            try{

                //自己当前的ip加端口
                String serverAddr=netConfig.getServerIp()+ Constants.IP_PORT_SPLITER+netConfig.getServerPort();
                //心跳信息
                String status=LOCALHOST_SITE+"#"+serverAddr+"#"+System.currentTimeMillis()+"#"+isFisrtSendHeartBeat+"#";

                //检查一下该server下收到的心跳，把自己的健康server列表更新
                checkHeartBeat();

                //发送心跳给自己
                onReceiveServerStatus(status);

                //如果这是第二次执行run方法了，即启动时已经发过心跳给其他server了，已经过了5秒，也已经把别的集群的同步信息复制到该注册内存表了
                //这时这个server也可以正常的使用netty来接收注册信息了
                if(!isFisrtSendHeartBeat){
                    log.info("可以正常接收注册信息了...");
                    ServerHandler.isStartFlag=true;
                }

                List<Server> allServers=servers;
                if(allServers.size()>0){
                    //给所有的server都发一次心跳
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
                isFisrtSendHeartBeat=false;
            }catch (Exception e) {
                log.error("发送server状态的过程中出现了错误:", e);
            } finally {
                //5s后再执行一次
                GlobalExecutor.registerServerStatusReporter(this, Constants.SERVER_STATUS_SYNCHRONIZATION_PERIOD_MILLIS);
            }
        }
    }

    /**
     * 检查健康与非健康的实例，更改健康server实例的列表
     */
    private void checkHeartBeat() {

        log.info("检查server集群间的心跳");

        List<Server> allServers=distroConfig.get(LOCALHOST_SITE);
        if(CollectionUtils.isEmpty(allServers)){
            return;
        }
        //System.out.println(allServers);
        List<Server> newHealthyList=new ArrayList<>(allServers.size());

        long now=System.currentTimeMillis();
        for(Server s:allServers){
            Long lastBeat=distroBeats.get(s.getKey());
            if(null==lastBeat){
                continue;
            }
            s.setAlive(now-lastBeat<Constants.SERVER_EXPIRED_MILLS);
            if(s.isAlive()&&!newHealthyList.contains(s)){
                //把存活的server实例加入健康的server列表
                newHealthyList.add(s);
            }
        }
        //和原来的健康server列表发生改变，就要修改健康server列表
        if(!CollectionUtils.isEqualCollection(healthyServers, newHealthyList)){
            healthyServers=newHealthyList;
            notifyListeners();
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

        //cluster_status#192.168.153.1:9002#1586336129841#
        String[] params=serverStatus.split("#");
        Server server=new Server();
        server.setSite(params[0]);
        server.setIp(params[1].split(Constants.IP_PORT_SPLITER)[0]);
        server.setServePort(Integer.parseInt(params[1].split(Constants.IP_PORT_SPLITER)[1]));
        server.setLastRefTime(Long.parseLong(params[2]));

        Long lastBeat=distroBeats.get(server.getKey());
        long now = System.currentTimeMillis();
        //是否第一次来发送心跳或者已经发过心跳但是重启了，是的话要先加入健康队列，然后进行同步数据
        boolean isFirst=(params[3].equals("true"));

        if(null!=lastBeat) {
            //不是第一次发送心跳,太久才发一次心跳(15s)的话也把该server节点设为非存活状态，等下次再发送一次心跳间隔小于15s才设置为存活状态
            server.setAlive(now - lastBeat < Constants.SERVER_EXPIRED_MILLS);
        }
//        }else{
//            isFirst=true;
//        }
        distroBeats.put(server.getKey(),now);

        Date date=new Date(Long.parseLong(params[2]));
        //格式化时间戳
        server.setLastRefTimeStr(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
        List<Server> list=distroConfig.get(server.getSite());
        if(list==null||list.size()<=0){
            list=new ArrayList<>();
            list.add(server);
            distroConfig.put(server.getSite(),list);
        }


        List<Server> tempServerList = new ArrayList<>();
        //更改原来存在distroConfig中server更新后的时间戳
        for(Server s:list){
            String serverId=s.getKey();
            String newServerId=server.getKey();
            //原来已经存在了
            if(serverId.equals(newServerId)){
                //更新发送心跳的server最新的数据
                tempServerList.add(server);
                continue;
            }
            //把不是发送心跳的server列表加回去
            tempServerList.add(s);
        }
        //上面的循环如果还没把当前发送心跳的server加进去，就在这里加进去
        if (!tempServerList.contains(server)) {
            tempServerList.add(server);
        }

        //覆盖原来的list
        distroConfig.put(server.getSite(),tempServerList);
        if(isFirst){
            //第一次发送心跳，把当前的server的内存注册表同步给该server,同时也同步一次给集群的其他server，跳过自己
            //因为进行集群同步时，只会发给健康的实例，这时因为还没有进行心跳检查，该server还没加入健康的列表里面去，所以在这里先进行心跳检查
            //不然该实例还没加入健康的实例，所以还是不会立刻发送
            checkHeartBeat();
            String serverAddr=netConfig.getServerIp()+ Constants.IP_PORT_SPLITER+netConfig.getServerPort();
            if(!server.getKey().equals(serverAddr)){
                consistencyService.notifyCluster(server.getKey());
            }
        }

    }

    /**
     * 返回健康的server实例列表
     * @return
     */
    public List<Server> getHealthyServers() {
        return healthyServers;
    }

    private void notifyListeners() {
        GlobalExecutor.notifyServerListChange(new Runnable() {
            @Override
            public void run() {
                //通知观察者集群列表更改了
                for (ServerChangeListener listener : listeners) {
                    listener.onChangeServerList(servers);
                    listener.onChangeHealthyServerList(healthyServers);
                }
            }
        });
    }
}
