package vip.firework;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("vip.firework.mapper")
public class SyjServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyjServiceApplication.class, args);
    }
}
