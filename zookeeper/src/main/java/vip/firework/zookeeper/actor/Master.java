package vip.firework.zookeeper.actor;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

@Component
public class Master implements Watcher {
    private static Logger logger = LoggerFactory.getLogger(Watcher.class);
    private static  ZooKeeper zk=null;
    @Value("${zookeeper.hostPort}")
    private String hostPort;
    private Random random = new Random();
    String serverId = Integer.toHexString(random.nextInt());
    public void startZk() throws IOException {
        if(zk == null){
            zk=new ZooKeeper(hostPort,15000,this);
        }
    }
    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.info("process watchedEvent {}",watchedEvent);
    }

    public void runForMaster(){
        while (true){
            try {
                zk.create("/master",
                        serverId.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL
                );
                zk.register(this);

            } catch (KeeperException e) {
                checkMaster();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(checkMaster()){
                break;
            }
        }

    }
    public boolean stopZk(){
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


    boolean isLeader=false;

    public boolean checkMaster(){
        while(true){
            try {
                Stat stat = new Stat();
                byte[] bytes = zk.getData("/master",false,stat);
                isLeader = new String(bytes).equals(serverId);
                return true;
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    public boolean isLeader(){
        return isLeader;
    }
    public static void main(String[] args){
        try {
            Master master = new Master();
            master.startZk();
            Thread.sleep(6000);
        } catch (Exception e) {
            logger.error("startZk error",e);
        }
    }
}
