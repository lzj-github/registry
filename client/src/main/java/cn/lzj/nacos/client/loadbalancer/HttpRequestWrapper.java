package cn.lzj.nacos.client.loadbalancer;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.net.URI;

public class HttpRequestWrapper implements HttpRequest{

    private final HttpRequest request;

    /**
     * 创建一个包装给定请求对象的新{@code HttpRequest}。request:请求要包装的请求对象
     */
    public HttpRequestWrapper(HttpRequest request) {
        Assert.notNull(request, "HttpRequest must not be null");
        this.request = request;
    }


    /**
     * 返回包装好的请求。
     */
    public HttpRequest getRequest() {
        return this.request;
    }

    /**
     * 返回包装请求的方法.
     */
    @Override
    @Nullable
    public HttpMethod getMethod() {
        return this.request.getMethod();
    }

    /**
     * 返回包装请求的方法值。
     */
    @Override
    public String getMethodValue() {
        return this.request.getMethodValue();
    }

    /**
     * 返回包装请求的URI。
     */
    @Override
    public URI getURI() {
        return this.request.getURI();
    }

    /**
     * 返回包装请求的头。
     */
    @Override
    public HttpHeaders getHeaders() {
        return this.request.getHeaders();
    }
}
