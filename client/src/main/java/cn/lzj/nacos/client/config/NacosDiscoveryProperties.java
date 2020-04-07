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
@ConfigurationProperties("nacos.discovery")
public class NacosDiscoveryProperties  {

    private String serverAddr;

    private String namespace;

    @Value("${spring.application.name}")
    private String service;

    private float weight = 1;

    private String clusterName ;

    private Map<String, String> metadata = new HashMap<>();

    //server的ip
    private String serverIp;

    private int serverPort = -1;

    @Value("${nacos.netty.server-addr}")
    private String nettyServerAddr;

    private String nettyServerIp;

    private int nettyServerPort;

    //当前服务的ip
    private String clientIp;

    //当前服务的端口
    @Value("${server.port}")
    private int clientPort;

    @PostConstruct
    public void init(){
        if(namespace==null){
            namespace= Constants.DEFAULT_NAMESPACE;
        }
        this.serverIp=serverAddr.split(":")[0];
        this.serverPort=Integer.valueOf(serverAddr.split(":")[1]);
        this.clusterName= Constants.DEFAULT_GROUP;
        nettyServerIp=nettyServerAddr.split(":")[0];
        nettyServerPort=Integer.valueOf(nettyServerAddr.split(":")[1]);
        try {
            //本机的ip
            clientIp= InetAddress.getLocalHost().getHostAddress().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
