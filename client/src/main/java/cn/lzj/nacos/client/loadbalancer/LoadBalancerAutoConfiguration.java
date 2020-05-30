package cn.lzj.nacos.client.loadbalancer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnClass(RestTemplate.class)
public class LoadBalancerAutoConfiguration {

//    @Autowired(required = false)
//    private List<RestTemplate> restTemplates = Collections.emptyList();
//
//    @Bean
//    @ConditionalOnMissingBean
//    public RestTemplateCustomizer restTemplateCustomizer(
//            final LoadBalancerInterceptor loadBalancerInterceptor) {
//        return restTemplate -> {
//            List<HandlerInterceptor> list = new ArrayList<>(
//                    restTemplate.getInterceptors());
//            list.add(loadBalancerInterceptor);
//            restTemplate.setInterceptors(list);
//        };
//    }
}
