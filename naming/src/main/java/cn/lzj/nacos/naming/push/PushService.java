package cn.lzj.nacos.naming.push;

import cn.lzj.nacos.naming.core.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PushService {

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    public void serviceChanged(Service service) {
        applicationEventPublisher.publishEvent(new ServiceChangeEvent(this,service));
    }
}
