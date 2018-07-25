package vip.firework.zookeeper.manager;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class ZookeeperManager{

    private static Logger logger = LoggerFactory.getLogger(ZookeeperManager.class);

    @Value("${zookeeper.hostPort}")
    private String hostPort;
    private static String staticHostPort;
    private static ZooKeeper zk=null;
    @PostConstruct
    public void init() {
        staticHostPort=hostPort;
    }
    private static ZooKeeper startZk(){
        if(zk == null){
            try {
                zk=new ZooKeeper(staticHostPort, 15000, new Watcher() {
                    @Override
                    public void process(WatchedEvent watchedEvent) {
                        logger.info("WatchedEvent {}",watchedEvent);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return zk;
    }
    public synchronized static ZooKeeper getZk(){
        if(zk==null){
            startZk();
        }
        return zk;
    }
    public synchronized static boolean stopZk(){
        if(zk == null){
            return false;
        }
        if(zk.getState().isConnected()){
            try {
                zk.close();
                zk=null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }else {
            return false;
        }
    }

}
