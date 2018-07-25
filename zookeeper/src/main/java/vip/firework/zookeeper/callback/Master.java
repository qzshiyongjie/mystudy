package vip.firework.zookeeper.callback;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vip.firework.zookeeper.manager.ZookeeperManager;

import java.util.*;

@Component
public class Master {
    private static Logger logger = LoggerFactory.getLogger(Master.class);
    static final String MASTER = "/master";
    static final String WORKERS = "/workers";
    static final String TASKS="/task";
    static final String ASSIGN="/assign";
    static final String STATUS="/status";
    private Random random = new Random();
    enum MasterStatus{
        RUNNING, ELECTED,NOTELECTED
    }
    MasterStatus state=MasterStatus.NOTELECTED;
    String serverId = Integer.toHexString(random.nextInt());
    List<String> workersCache=new ArrayList<>();//从节点列表的本地缓存
    public List<String> getChildrenPath(String path){
        try {
            return ZookeeperManager.getZk().getChildren(path,false);
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    public void runForMaster(){
        ZookeeperManager.getZk().create(MASTER, serverId.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE
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
        ZookeeperManager.getZk().getData(MASTER, false, new AsyncCallback.DataCallback() {
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
    /**
     * 监测/master节点状况，如果/master节点被删除，尝试注册/master节点
     */
    void masterExits(){
        ZookeeperManager.getZk().exists(MASTER, new Watcher() {
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

    /**
     * 执行lead任务
     */
    void takeLeaderShip(){
        //创建开始需要的节点
        bootstrap();
        //监测worker状态
        getWorkers();
        //监测task状态
        getTasks();
    };


    void getWorkers(){
        ZookeeperManager.getZk().getChildren(WORKERS, new Watcher() {
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
                        getWorkers();
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


    /**
     * 这个地方加入本地缓存
     * @param children
     */
    void reassignAndSet(List<String> children){
        List<String> toProcess=new ArrayList<>();
        Collections.sort(children);
        if(workersCache.size()==0){
            workersCache.clear();
            workersCache.addAll(children);
        }else {
            toProcess=removeAndSet(children);
        }
        if(toProcess.size()>0){
            //存在失效的worker
            for(String worker:children){
                reAssignTask(worker);
            }
        }
    }

    /**
     * 对于已经无效的work,如果该worker有未完成的任务，将任务分配给其他worker
     */
    void reAssignTask(String workerPath){
        try {
            List<String> tasks = ZookeeperManager.getZk().getChildren(ASSIGN+workerPath,false);
            if(tasks != null && tasks.size()>0){
                assignTask(tasks);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    void getTasks(){
        ZookeeperManager.getZk().getChildren(TASKS, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
                    if(TASKS.equals(watchedEvent.getPath())){
                        getTasks();
                    }
                }
            }
        }, new AsyncCallback.ChildrenCallback() {
            @Override
            public void processResult(int i, String s, Object o, List<String> list) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        getTasks();
                        break;
                    }
                    case OK:{
                        if(list != null){
                            //存在task时给worker分配任务
                            assignTask(list);
                        }
                        break;
                    }
                    default:{
                        logger.error("getTasks error",
                                KeeperException.create(KeeperException.Code.get(i),s));
                    }
                }
            }
        },null);
    }
    void assignTask(List<String> tasks){
        for(String task:tasks){
            getTaskData(task);
        }
    }
    void getTaskData(String task){
        ZookeeperManager.getZk().getData(TASKS + task, false, new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        getTaskData((String) o);
                        break;
                    }
                    case OK:{
                        //本地缓存取workers
                        int randomWorker = random.nextInt(workersCache.size());
                        String workerNodePath = workersCache.get(randomWorker);
                        String assignPath = ASSIGN+workerNodePath+"/"+o;
                        createAssignment(assignPath,bytes);
                    }
                    default:{
                        logger.error("error when trying to get task data",
                                KeeperException.create(KeeperException.Code.get(i),new String(bytes)));
                    }
                }
            }
        },task);
    }

    /**
     * 分配任务
     * @param path
     * @param data
     */
    void createAssignment(String path,byte[] data){
        ZookeeperManager.getZk().create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, new AsyncCallback.StringCallback() {
            @Override
            public void processResult(int i, String s, Object o, String s1) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        createAssignment(s,(byte[]) o);
                        break;
                    }
                    case OK:{
                        logger.info(" task assign success {}",s);
                        //删除已经分配 task 节点
                        deleteTask(s.substring(s.lastIndexOf("/")+1));
                    }
                }
            }
        },data);
    }
    void deleteTask(String path){
        try {
            ZookeeperManager.getZk().delete(path,-1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    /**
     * TODO
     * 对于已经死掉的work把task重新分配给其他worker
     * @param worker
     */
    void getAbsentWorkerTask(List<String> worker){

    }

    /**
     * 检查节点是否存在
     * @param children
     * @return
     */
    List<String> removeAndSet(List<String> children){
        List<String> result = new ArrayList<>();
        for(String child:children){
            Stat stat = new Stat();
            try {
                byte[] datas= ZookeeperManager.getZk().getData(child,false,stat);
                if(datas == null || datas.length==0){
                    result.add(child);
                }
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public boolean isLeader(){
        return state.equals(MasterStatus.ELECTED);
    }
    public void bootstrap(){
        createParent(WORKERS,new byte[0]);
        createParent(ASSIGN,new byte[0]);
        createParent(TASKS,new byte[0]);
        createParent(STATUS,new byte[0]);
    }
    public void createParent(String path,byte[] data){
        ZookeeperManager.getZk().create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT, new AsyncCallback.StringCallback() {
                    @Override
                    public void processResult(int i, String s, Object o, String s1) {
                        logger.info("master createParent,code {} ,path{} ,data{}",i,s,o);
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
}
