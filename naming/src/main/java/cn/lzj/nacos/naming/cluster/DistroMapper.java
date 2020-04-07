package cn.lzj.nacos.naming.cluster;

import cn.lzj.nacos.api.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

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
    }
}
