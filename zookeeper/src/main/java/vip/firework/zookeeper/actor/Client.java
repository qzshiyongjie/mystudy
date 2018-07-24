package vip.firework.zookeeper.actor;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Random;

public class Client implements Watcher{
    private static Logger logger = LoggerFactory.getLogger(Client.class);
    private static ZooKeeper zk=null;
    @Value("${zookeeper.hostPort}")
    private String hostPort;
    public void startZk() throws IOException {
        if (zk == null) {
            zk = new ZooKeeper(hostPort, 15000, this);
        }
    }
    public String queueCommand(String command) throws KeeperException, InterruptedException {
        while (true){
            String name = null;
            name = zk.create("/tasks/task-",command.getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT_SEQUENTIAL
                    );
            return name;
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}
