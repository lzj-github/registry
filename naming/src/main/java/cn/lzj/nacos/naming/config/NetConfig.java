package cn.lzj.nacos.naming.config;


import cn.lzj.nacos.api.common.Constants;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Data
@Component
@ConfigurationProperties("netty")
public class NetConfig {

    private String addr;

    private String nettyIp;

    private int nettyPort;

    private String serverIp;

    @Value("${server.port}")
    private int serverPort=-1;

    @Value("${spring.application.name}")
    private String name;


    @PostConstruct
    public void init(){
        nettyIp=addr.split(":")[0];
        nettyPort= Integer.parseInt(addr.split(":")[1]);
        try {
            serverIp=InetAddress.getLocalHost().getHostAddress().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //System.out.println("netty:"+nettyIp+":"+nettyPort+"   server:"+serverIp+":"+serverPort);
        //netty:localhost:9001   server:192.168.153.1:9000
    }


}
