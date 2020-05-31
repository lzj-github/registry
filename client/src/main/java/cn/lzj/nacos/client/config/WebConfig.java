package cn.lzj.nacos.client.config;

import cn.lzj.nacos.client.loadbalancer.IRule;
import cn.lzj.nacos.client.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 测试替换默认策略
     * @return
     */
  //  @Bean
    public IRule iRule(){
        return new RandomRule();
    }
}
