package vip.firework.zookeeper;

import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.firework.zookeeper.actor.Master;

import java.io.IOException;

@SpringBootApplication
@RestController
public class ZookeeperApplication {
    private static Logger logger = LoggerFactory.getLogger(ZookeeperApplication.class);
    @Autowired
    private Master master;
    @RequestMapping("/startZkmaster")
    public String startZkMaster(){
        try {
            master.startZk();
            master.runForMaster();
            if(master.isLeader()){
                logger.info("I am leader");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "success";
    }
    @RequestMapping("/stopZkmaster")
    public String stopZkmaster(){
        if(master.stopZk())
            return "success";
        else return "fail";
    }

    public static void main(String[] args) {
        SpringApplication.run(ZookeeperApplication.class, args);
    }
}
