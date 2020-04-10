package cn.lzj.nacos.client.config;


import cn.lzj.nacos.api.common.Constants;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties("registry.discovery")
public class DiscoveryProperties {

    private String namespace;

    private String clusterName;

    @Value("${spring.application.name}")
    private String service;

    private Map<String, String> metadata = new HashMap<>();

    private String serverAddr;

    @Value("${registry.netty.server-addr}")
    private String nettyServerAddr;

    //服务端的ip映射netty端的ip，用来选择server连接时用到
    //Map<serverIp,nettyIp>
    private Map<String,String> mappingMap=new HashMap<>();


    //当前服务的ip
    private String clientIp;

    //当前服务的端口
    @Value("${server.port}")
    private int clientPort;

    @PostConstruct
    public void init() throws UnknownHostException {
        if(namespace==null){
            namespace= Constants.DEFAULT_NAMESPACE;
        }
        if(clusterName==null){
            clusterName= Constants.DEFAULT_GROUP;
        }

        //证明有集群
        if(serverAddr.contains(",")&&nettyServerAddr.contains(",")){
            String[] servers = serverAddr.split(",");
            String[] nettyServers=nettyServerAddr.split(",");
            for(int i=0;i<servers.length;i++){
                if(servers[i].startsWith("localhost")){
                    //localhost换成192.168.153.1这种
                    servers[i]=InetAddress.getLocalHost().getHostAddress().toString()+":"+servers[i].split(":")[1];
                }
                if(nettyServers[i].startsWith("localhost")){
                    nettyServers[i]=InetAddress.getLocalHost().getHostAddress().toString()+":"+nettyServers[i].split(":")[1];
                }
                mappingMap.put(servers[i],nettyServers[i]);
            }
        }else{
            //只写了一台server的ip
            if(serverAddr.startsWith("localhost")){
                serverAddr=InetAddress.getLocalHost().getHostAddress().toString()+":"+serverAddr.split(":")[1];
            }
            if(nettyServerAddr.startsWith("localhost")){
                nettyServerAddr=InetAddress.getLocalHost().getHostAddress().toString()+":"+nettyServerAddr.split(":")[1];
            }
            mappingMap.put(serverAddr,nettyServerAddr);
        }

        //本机的ip
        clientIp= InetAddress.getLocalHost().getHostAddress().toString();

    }

}
