package cn.lzj.nacos.client.loadbalancer;

import cn.lzj.nacos.api.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;

import java.net.URI;
import java.net.URISyntaxException;
@Slf4j
public class ServiceRequestWrapper extends HttpRequestWrapper {
    private Instance instance;
    public ServiceRequestWrapper(HttpRequest request, Instance instance) {
        super(request);
        this.instance=instance;
    }

    @Override
    public URI getURI() {
        URI uri = reconstructURI(this.instance, getRequest().getURI());
        return uri;
    }

    private URI reconstructURI(Instance instance, URI uri) {
        String serviceName = uri.getHost();
        String oldPath = uri.toString();
        String contextPath=oldPath.substring(("http://" + serviceName).length());
        String newPath = instance.getUri().toString() + contextPath;
        log.info("通过拦截器替换后的url:"+newPath);
        try {
            URI newURI = new URI(newPath);
            return newURI;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
}
