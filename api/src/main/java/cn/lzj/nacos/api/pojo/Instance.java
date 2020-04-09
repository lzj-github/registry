package cn.lzj.nacos.api.pojo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Instance {
    private String namespaceId;

    private String ip;

    private int port;

    private String serviceName;

    private String clusterName;

    private Map<String, String> metadata = new HashMap<String, String>();

    //重写hasCode和equals方法
    @Override
    public int hashCode() {
        return getIp().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || obj.getClass() != getClass()) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Instance other = (Instance) obj;

        //namespaceId,serviceName,ip，port相同就认为两个对象相等
        return getIp().equals(other.getIp()) && (getPort() == other.getPort() )
                && getNamespaceId().equals(other.namespaceId)&&getServiceName().equals(other.getServiceName());
    }
}
