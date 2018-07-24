package vip.firework.zookeeper.callback;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;
@Component
public class CallbackWorker implements Watcher {
    private static Logger logger = LoggerFactory.getLogger(CallbackWorker.class);
    private static ZooKeeper zk=null;
    @Value("${zookeeper.hostPort}")
    private String hostPort;
    private Random random = new Random();
    volatile String status;
    String serverId = Integer.toHexString(random.nextInt());
    private String name = "/worker-" + serverId;
    public void startZk() throws IOException {
        if(zk == null){
            zk=new ZooKeeper(hostPort,15000,this);
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
    public void  register(){
        zk.create("/workers"+name,
                "Idle".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL,
                new AsyncCallback.StringCallback() {
                    @Override
                    public void processResult(int i, String s, Object o, String s1) {
                        switch (KeeperException.Code.get(i)){
                            case CONNECTIONLOSS:{
                                register();
                                break;
                            }
                            case OK:{
                                logger.info("register successful:{}",serverId);
                                break;
                            }
                            case NODEEXISTS:{
                                logger.warn("Already registered:{}",serverId);
                                break;
                            }
                            default:{
                                logger.error("something wang wrong: {}",KeeperException.create(KeeperException.Code.get(i)));
                                break;
                            }
                        }
                    }
                },null);
    }

    public void setStatus(String status){
        this.status=status;
        updateStatus(status);
    }

    synchronized private void updateStatus(String status) {
        if(status.equals(this.status)){
            zk.setData("/workers"+name, status.getBytes(), -1, new AsyncCallback.StatCallback() {
                @Override
                public void processResult(int i, String s, Object o, Stat stat) {
                   if(KeeperException.Code.get(i)== KeeperException.Code.CONNECTIONLOSS){
                       updateStatus(o.toString());
                   }
                }
            },status);
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.info("watch process {}",watchedEvent);
    }
}
