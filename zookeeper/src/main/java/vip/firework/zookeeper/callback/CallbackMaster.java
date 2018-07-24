package vip.firework.zookeeper.callback;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Random;
@Component
public class CallbackMaster implements Watcher {
    private static Logger logger = LoggerFactory.getLogger(CallbackMaster.class);
    static final String MASTER = "/master";
    static final String WORKERS = "/workers";
    private static ZooKeeper zk=null;
    @Value("${zookeeper.hostPort}")
    private String hostPort;
    private Random random = new Random();
    enum MasterStatus{
        RUNNING, ELECTED,NOTELECTED
    }
    MasterStatus state;
    String serverId = Integer.toHexString(random.nextInt());
    public void startZk() throws IOException {
        if(zk == null){
            zk=new ZooKeeper(hostPort,15000,this);
        }
    }
    public void runForMaster(){
        zk.create(MASTER, serverId.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE
                , CreateMode.EPHEMERAL, new AsyncCallback.StringCallback() {
                    @Override
                    public void processResult(int i, String s, Object o, String s1) {
                        logger.info("masterCheckData,code {} ,path{} ,data{}",i,s,o);
                        switch (KeeperException.Code.get(i)){
                            case CONNECTIONLOSS:{
                                //链接丢失时查询主节点是否创建
                                checkMaster();
                                return;
                            }
                            case OK:{
                                state = MasterStatus.ELECTED;
                                takeLeaderShip();
                                break;
                            }
                            case NODEEXISTS:{
                                state = MasterStatus.NOTELECTED;
                                masterExits();
                                break;
                            }
                            default:{
                                state=MasterStatus.NOTELECTED;
                                logger.error("create master error",KeeperException.create(KeeperException.Code.get(i),s));
                                break;
                            }

                        }
                        logger.info("i am leader ? {}",state.equals(MasterStatus.ELECTED)?"yes":"no");
                    }
                }, null);
    }
    //检查/master节点是否存在,不存在时创建/master节点
    public void checkMaster(){
        zk.getData(MASTER, false, new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
                logger.info("masterCheckData,code {} ,path{} ,data{}",i,s,new String(bytes));
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        checkMaster();
                        break;
                    }
                    case NONODE:{
                        runForMaster();
                        break;
                    }
                }
            }
        }, null);
    }
    void takeLeaderShip(){

    };

    /**
     * 监测/master节点状况，如果/master节点被删除，尝试注册/master节点
     */
    void masterExits(){
        zk.exists(MASTER, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                //主节点删除状态，重新注册主节点
                if(watchedEvent.getType() == Event.EventType.NodeDeleted){
                    if("/master".equals(watchedEvent.getPath())) {
                        runForMaster();
                    }
                }
            }
        }, new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int i, String s, Object o, Stat stat) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        masterExits();
                        break;
                    }
                    case OK:{
                        // /master节点不存在，竞选/master节点
                        if(stat==null){
                            state = MasterStatus.RUNNING;
                            runForMaster();
                        }
                        break;
                    }
                    default:{
                        //意外情况，监测master节点是否存在
                        checkMaster();
                        break;
                    }
                }
            }
        },null);
    }
    void getWorkers(){
        zk.getChildren(WORKERS, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
                    if(WORKERS.equals(watchedEvent.getPath())){
                        getWorkers();
                    }
                }
            }
        }, new AsyncCallback.ChildrenCallback() {
            @Override
            public void processResult(int i, String s, Object o, List<String> list) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        getworkerList();
                        break;
                    }
                    case OK:{
                        logger.error("successfull got a list of worker size:{}",list.size());
                        reassignAndSet(list);
                    }
                }
            }
        },null);
    }
    //TODO
    void getworkerList(){

    }
    //TODO
    void reassignAndSet(List<String> child){

    }

    public boolean isLeader(){
        return state.equals(MasterStatus.ELECTED);
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
    public void bootstrap(){
        createParent("/workers",new byte[0]);
        createParent("/assign",new byte[0]);
        createParent("/tasks",new byte[0]);
        createParent("/status",new byte[0]);
    }
    public void createParent(String path,byte[] data){
        zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT, new AsyncCallback.StringCallback() {
                    @Override
                    public void processResult(int i, String s, Object o, String s1) {
                        logger.info("masterCheckData,code {} ,path{} ,data{}",i,s,o);
                        switch (KeeperException.Code.get(i)){
                            case CONNECTIONLOSS:{
                                createParent(s,(byte[])o);
                                break;
                            }
                            case OK:{
                                logger.info("parentcreate path:{}",s);
                                break;
                            }
                            case NODEEXISTS:{
                                logger.info("parent already registered: ",s);
                                break;
                            }
                            default:{
                                logger.error("something wang wrong: {}",KeeperException.create(KeeperException.Code.get(i),s));
                                break;
                            }
                        }
                    }
                }, data);
    }
    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}
