package cn.lzj.nacos.naming.push;

import cn.lzj.nacos.naming.core.Service;
import org.springframework.context.ApplicationEvent;

/**
 * 服务发生改变的事件对象
 */
public class ServiceChangeEvent extends ApplicationEvent {
    private Service service;

    public ServiceChangeEvent(Object source, Service service) {
        super(source);
        this.service = service;
    }

    public Service getService() {
        return service;
    }
}
