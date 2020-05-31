package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.api.pojo.ServiceInfo;
import cn.lzj.nacos.client.core.HostReactor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class LoadBalancerInterceptor implements ClientHttpRequestInterceptor {

    @Autowired
    private HostReactor hostReactor;

    @Autowired
    private ILoadBalancer loadBalancer;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        URI originUri = request.getURI();
        String serviceName=originUri.getHost();
        //拿到该服务
        ServiceInfo serviceInfo0 = hostReactor.getServiceInfo0(serviceName);
        if(serviceInfo0==null){
            log.warn("没有该服务名称或者采用了硬编码格式，推荐使用服务名来当主机名");
            return execution.execute(request, body);
        }
        List<Instance> instances =  serviceInfo0.getInstances();
        //通过负载均衡算法选出一个要调用的client
        Instance instance=loadBalancer.chooseInstance(instances);
        log.info("通过负载均衡策略选出的实例:"+instance);

        //包装新的request，替换url
        HttpRequest serviceRequest = new ServiceRequestWrapper(request, instance);
        //偷龙转凤后放行
        return execution.execute(serviceRequest, body);
    }

    @Bean
    @ConditionalOnMissingBean(IRule.class)//当给定的在bean不存在时,则实例化当前Bean
    public IRule iRule(){
        return new RoundRobinRule();
    }
}
