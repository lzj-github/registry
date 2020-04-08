package cn.lzj.nacos.naming.cluster;

import cn.lzj.nacos.api.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("distroMapper")
public class DistroMapper implements ServerChangeListener{

    private List<String> healthyList = new ArrayList<>();

    @Autowired
    private ServerListManager serverListManager;

    @PostConstruct
    public void init() {
        //注册观察者
        serverListManager.listen(this);
    }

    @Override
    public void onChangeServerList(List<Server> servers) {

    }

    @Override
    public void onChangeHealthyServerList(List<Server> latestReachableMembers) {

        List<String> newHealthyList = new ArrayList<>();
        for (Server server : latestReachableMembers) {
            newHealthyList.add(server.getIp()+ Constants.IP_PORT_SPLITER+server.getServePort());
        }
        healthyList = newHealthyList;
        log.info("健康的server列表更改了 :"+healthyList);
    }
}
