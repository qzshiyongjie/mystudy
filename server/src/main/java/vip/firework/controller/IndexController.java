package vip.firework.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private DiscoveryClient client;

    @RequestMapping("/index")
    public String index(){
        ServiceInstance serviceInstance= client.getLocalServiceInstance();
        logger.info("service host:{},port:{}",serviceInstance.getHost(),serviceInstance.getPort());
        return "hello word";
    }
}
