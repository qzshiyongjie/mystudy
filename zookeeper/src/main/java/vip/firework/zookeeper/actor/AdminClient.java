package vip.firework.zookeeper.actor;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

public class AdminClient implements Watcher {
    private static Logger logger = LoggerFactory.getLogger(AdminClient.class);
    private static ZooKeeper zk=null;
    @Value("${zookeeper.hostPort}")
    private String hostPort;
    public void startZk() throws IOException {
        if (zk == null) {
            zk = new ZooKeeper(hostPort, 15000, this);
        }
    }
    public void listState(){
        logger.info("master:");
        Stat stat = new Stat();
        try {
            byte[] masterData = zk.getData("/master",false,stat);
            logger.info("masterData {}",new String(masterData));
            logger.info("worker:");
            for(String s:zk.getChildren("/workers",false)){
                String data = new String(zk.getData("/workers"+s,false,null));
                logger.info("worker data:{}",data);
            }
            logger.info("tasks:");
            for(String s:zk.getChildren("/assign",false)){
                logger.info("task {}",s);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}
