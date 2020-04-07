package cn.lzj.nacos.naming.boot;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component("applicationContext")
public class SpringContext implements ApplicationContextAware {

    public  static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static ApplicationContext getAppContext() {
        //拿到spring上下文容器，方便后面调用来拿取bean
        return context;
    }
}
