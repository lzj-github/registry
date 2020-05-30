package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;
import cn.lzj.nacos.api.pojo.ServiceInfo;
import cn.lzj.nacos.client.core.HostReactor;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class LoadBalancerInterceptor implements HandlerInterceptor {

    @Autowired
    private HostReactor hostReactor;

    @Autowired
    private ILoadBalancer loadBalancer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpRequest httpRequest= (HttpRequest) request;
        URI originUri = httpRequest.getURI();
        String serviceName=originUri.getHost();
        //拿到该服务的所有实例
        ServiceInfo serviceInfo0 = hostReactor.getServiceInfo0(serviceName);
        List<Instance> instances =  serviceInfo0.getInstances();
        //通过负载均衡算法选出一个要调用的client
        Instance instance=loadBalancer.chooseInstance(instances);
        if(instance!=null){
            String path=originUri.getPath();
            //替换url
            path.replace("http://"+serviceName,instance.getIp());
            request.getRequestDispatcher(path).forward(request,response);
        }

        return true;
    }

}
