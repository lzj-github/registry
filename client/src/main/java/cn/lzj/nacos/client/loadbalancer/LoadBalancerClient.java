package cn.lzj.nacos.client.loadbalancer;

import java.io.IOException;

public interface LoadBalancerClient {

    <T> T execute(String serviceId) throws IOException;
}
