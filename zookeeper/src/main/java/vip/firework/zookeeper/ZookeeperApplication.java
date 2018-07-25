package vip.firework.zookeeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import vip.firework.zookeeper.callback.Master;
import vip.firework.zookeeper.callback.Worker;
import vip.firework.zookeeper.manager.ZookeeperManager;

import java.io.IOException;

@SpringBootApplication
@RestController
public class ZookeeperApplication {
    private static Logger logger = LoggerFactory.getLogger(ZookeeperApplication.class);
    @Autowired
    private Master master;
    @Autowired
    private Worker worker;
    @Autowired
    private ZookeeperManager zookeeperManager;
    @RequestMapping("/startZkmaster")
    public String startZkMaster(){
        master.runForMaster();
        master.bootstrap();
        if(master.isLeader()){
            logger.info("I am leader");
        }
        return "success";
    }
    @RequestMapping("/startClient/{id}")
    public String startClient(@PathVariable("id") Integer id){
        worker.changeWorker(id);
        worker.register();
        return "success";
    }

    @RequestMapping("/showChildren/{path}")
    @ResponseBody
    public Object showChildren(@PathVariable("path") String path){
        return master.getChildrenPath(path);
    }
    @RequestMapping("/stopZkmaster")
    public String stopZkmaster(){
        if(ZookeeperManager.stopZk())
            return "success";
        else return "fail";
    }
    @RequestMapping("/checkMaster")
    public String checkMaster(){
        master.checkMaster();
        return "success";
    }

    public static void main(String[] args) {
        SpringApplication.run(ZookeeperApplication.class, args);
    }
}
