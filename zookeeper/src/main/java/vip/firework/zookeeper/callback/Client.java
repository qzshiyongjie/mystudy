package vip.firework.zookeeper.callback;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vip.firework.zookeeper.manager.ZookeeperManager;

/**
 * 客户段
 * 1：提交任务
 * 2：监听任务完成状态
 */
@Component
public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class);
    static final String TASKS="/tasks/task-";
    static final String STATUS = "/status";
    static final String STATUS_TASK = "/status/task-";
    void submitTask(String task){
        ZookeeperManager.getZk().create(TASKS, task.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL,
                new AsyncCallback.StringCallback() {
                    @Override
                    public void processResult(int i, String s, Object o, String s1) {
                        switch (KeeperException.Code.get(i)){
                            case CONNECTIONLOSS:{
                                submitTask(o.toString());
                                break;
                            }
                            case OK:{
                                logger.info("my create task name:{}",s);
                                String path = STATUS+"/"+s.substring(s.lastIndexOf("/")+1);
                                watchStatus(path,task);
                                break;
                            }
                            default:{
                                logger.error("something error when create task",KeeperException
                                .create(KeeperException.Code.get(i),s));
                            }
                        }
                    }
                }
                , task);

    }
    void watchStatus(String path,String task){
        ZookeeperManager.getZk().exists(path, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if(watchedEvent.getType()==Event.EventType.NodeChildrenChanged){
                    if(watchedEvent.getPath().contains(STATUS_TASK)){
                        getTaskStatus(path,task);
                    }
                }
            }
        }, new AsyncCallback.StatCallback() {
            @Override
            public void processResult(int i, String s, Object o, Stat stat) {
                switch (KeeperException.Code.get(i)){
                    case CONNECTIONLOSS:{
                        watchStatus(s,o.toString());
                        break;
                    }
                    case OK:{
                        if(stat != null){
                            getTaskStatus(s,o.toString());
                        }
                        break;
                    }
                    case NONODE:{
                        break;
                    }
                    default:{
                        logger.error("something error when watchStatus",KeeperException
                                .create(KeeperException.Code.get(i),s));
                        break;
                    }
                }

            }
        },task);
    }
    void getTaskStatus(String path,String task){
        ZookeeperManager.getZk().getData(path, false, new AsyncCallback.DataCallback() {
            @Override
            public void processResult(int i, String s, Object o, byte[] bytes, Stat stat) {
                logger.info("task_status path:{} status:{}",s,new String(bytes));
            }
        },task);
    }

}
