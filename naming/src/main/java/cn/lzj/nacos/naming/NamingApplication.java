package cn.lzj.nacos.naming;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NamingApplication {

    public static void main(String[] args) {
        SpringApplication.run(NamingApplication.class, args);
    }

    @Bean
    public Redisson redisson(){
        //此为单机模式
        Config config=new Config();
        config.useSingleServer().setAddress("redis://39.108.141.134:6379").setPassword("123456").setDatabase(0);
        return (Redisson) Redisson.create(config);
    }

}
