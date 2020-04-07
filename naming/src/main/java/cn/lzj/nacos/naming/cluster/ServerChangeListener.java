package cn.lzj.nacos.naming.cluster;

import java.util.List;

public interface ServerChangeListener {

    /**
     * 如果成员列表更改，则调用此方法
     */
    void onChangeServerList(List<Server> servers);

    /**
     * 如果可用的成员列表更改，则调用此方法
     */
    void onChangeHealthyServerList(List<Server> healthyServer);
}
