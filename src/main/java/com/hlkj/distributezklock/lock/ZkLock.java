package com.hlkj.distributezklock.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Lixiangping
 * @createTime 2022年03月26日 21:53
 * @decription:
 *      AutoCloseable接口：自动释放锁。释放锁后会‘回调’下一个节点的监听（这里是唤醒了下一个节点）
 *      Watcher接口：监听器监听前一个节点（删除）操作，并唤醒了下一个节点所在线程
 */
@Slf4j
public class ZkLock implements AutoCloseable, Watcher {

    private ZooKeeper zooKeeper;
    private String zNode;

    public ZkLock() throws IOException {
        this.zooKeeper = new ZooKeeper("127.0.0.1:2181", 10000, this);
    }

    public boolean getLock(String businessCode){
        try {
            //创建业务根节点
            Stat stat = zooKeeper.exists("/" + businessCode, false);
            if (stat==null){
                zooKeeper.create("/" + businessCode, businessCode.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);//区分业务，和锁没多大关系，所以选择持久节点
            }
            //创建瞬时有序节点
            zNode = zooKeeper.create("/" + businessCode + "/" + businessCode + "_", businessCode.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_SEQUENTIAL);
            //获取业务节点下所有的子节点
            List<String> childrenNodes = zooKeeper.getChildren("/" + businessCode, false);
            Collections.sort(childrenNodes);//升序
            String firstNode = childrenNodes.get(0);//获取序号最小的子节点（第一个）
            //如果当前创建的节点是第一个子节点，获得锁
            if (zNode.endsWith(firstNode)){
                return true;
            }
            //如果不是第一个子节点，则监听前一个节点
            String lastNode = firstNode;
            for (String node:childrenNodes){
                if (zNode.endsWith(node)){
                    //创建监听器监听前一个节点
                    zooKeeper.exists("/" + businessCode + "/" + lastNode, true);
                    break;
                }else{
                   lastNode = node;
                }
            }
            //等待前一个节点消失，执行process(WatchedEvent watchedEvent)方法唤起当前线程
            synchronized (this){
                wait();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        //类型：节点删除。回调下一个节点的监听方法process(WatchedEvent watchedEvent)唤醒下一个节点所在线程
        zooKeeper.delete(zNode, -1);
        zooKeeper.close();
        log.info("释放了锁~");
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        //类型：节点删除
        if (watchedEvent.getType() == Event.EventType.NodeDeleted) {
            synchronized (this){
                notify();//唤醒当前节点
            }
        }
    }
}
