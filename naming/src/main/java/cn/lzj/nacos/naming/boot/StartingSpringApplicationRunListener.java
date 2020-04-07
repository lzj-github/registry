package cn.lzj.nacos.naming.boot;

import cn.lzj.nacos.naming.utils.SystemUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class StartingSpringApplicationRunListener implements SpringApplicationRunListener {

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        logClusterConf();
    }

    private void logClusterConf() {
        try {
            List<String> clusterConf = SystemUtils.readClusterConf();
            log.info("The server IP list of Nacos is {}", clusterConf);
        } catch (IOException e) {
            log.error("read cluster conf fail", e);
        }
    }
}
