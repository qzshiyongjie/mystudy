package vip.firework.zookeeper.callback;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vip.firework.zookeeper.manager.ZookeeperManager;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class Worker {
    private static Logger logger = LoggerFactory.getLogger(Worker.class);
    static final String ASSIGN_WORKER="/assign/worker-";
    static final String STATUS="/status";
    @Value("${zookeeper.hostPort}")
    private String hostPort;
    private Random random = new Random();
    volatile String status;
    String serverId = Integer.toHexString(0);
    private String name = "/worker-" + serverId;

    public void changeWorker(int id){
        serverId = Integer.toHexString(id);
        name= "/worker-" + serverId;
    }
    public void  register(){
        ZookeeperManager.getZk().create("/workers"+name,
                new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL,
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
                                doWorder();
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
    void doWorder(){
        getAssignTasks();
    }
    void getAssignTasks(){
        ZookeeperManager.getZk().getChildren(ASSIGN_WORKER+ serverId, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
                    if((ASSIGN_WORKER+serverId).equals(watchedEvent.getPath())){
                        getAssignTasks();
                    }
                }
            }
        }, new AsyncCallback.ChildrenCallback() {
            @Override
            public void processResult(int i, String s, Object o, List<String> list) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        getAssignTasks();
                        break;
                    }
                    case OK:{
                        if(list != null){
                            logger.info("looping into task");
                            executeTask(list);
                        }
                    }
                }
            }
        },null);
    }
    void executeTask(final List<String> children){
        Executor executor = Executors.newScheduledThreadPool(5);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Stat stat = new Stat();
                for(String task:children){
                    String path = ASSIGN_WORKER+serverId+"/"+task;
                    try {
                        byte[] datas= ZookeeperManager.getZk().getData(path,false,stat);
                        if(datas!=null && datas.length>0){
                            logger.info("task: {} has bend done status: {}",task,new String(datas));
                        }else {
                            logger.info("task {} doing",task);
                            ZookeeperManager.getZk().setData(path,"DONE".getBytes(),-1);
                            logger.info("task{} do ok status {}",new String(ZookeeperManager.getZk().getData(path,false,stat)));
                            ZookeeperManager.getZk().create(STATUS+"/"+task,"DONE".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
                        }
                    } catch (KeeperException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void setStatus(String status){
        this.status=status;
        updateStatus(status);
    }

    synchronized private void updateStatus(String status) {
        if(status.equals(this.status)){
            ZookeeperManager.getZk().setData("/workers"+name, status.getBytes(), -1, new AsyncCallback.StatCallback() {
                @Override
                public void processResult(int i, String s, Object o, Stat stat) {
                    switch (KeeperException.Code.get(i)){
                        case CONNECTIONLOSS:{
                            updateStatus(o.toString());
                            break;
                        }
                        case OK:{

                        }
                    }
                }
            },status);
        }
    }
}
