package cn.lzj.nacos.client.api;

public interface NamingService {

    public void registerInstance(String serviceName, String groupName , String ip, int port,String nameSpaceId);
}
