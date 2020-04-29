package cn.lzj.nacos.naming.push;

import cn.lzj.nacos.naming.core.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class PushService {

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    /**
     * 有service的实例改变了
     * @param service
     */
    public void serviceChanged(Service service,String messageId) {
        applicationEventPublisher.publishEvent(new ServiceChangeEvent(this,service,messageId));
    }

}
