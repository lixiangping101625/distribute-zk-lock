### 基于Zookeeper实现分布式锁
#### 基础理论：
    利用zookeeper的瞬时节点的而有序性（多线程并发访问创建顺势节点时，得到的序列是有序的）
#### zookeeper分布式实现原理（思路）
    1、多个线程访问创建瞬时序列，序号递增
    2、创建序列时序号最小的线程获得锁（执行完成后，删除自己序号对应的节点）
    3、其他线程监听自己前一个序列中前一个序号的数据状态（其实就是瞬时节点的删除事件），唤醒当前线程执行后操作
    4、以此类推

### 方式1、spring boot集成zookeeper自己实现分布式锁
    1、依赖
        <!--Zookeeper：版本和安装的一致-->
        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <version>3.8.0</version>
        </dependency>
    2、编写ZkLock.java
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
    3、编写api进行测试
        import com.hlkj.distributezklock.lock.ZkLock;
        import lombok.extern.slf4j.Slf4j;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RestController;
        
        import java.io.IOException;
        
        /**
         * @author Lixiangping
         * @createTime 2022年03月26日 22:23
         * @decription:
         */
        @Slf4j
        @RestController
        public class ZookeeperController {
        
            /**
             * 测试zookeeper分布式锁
             * @return
             */
            @GetMapping("/zkLock")
            public String ZookeeperLock() {
                log.info("进入了方法~");
                try(ZkLock zkLock = new ZkLock()){
                    if (zkLock.getLock("order")){
                        log.info("获得了锁");
                        Thread.sleep(10000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.info("方法执行完成");
                return "方法执行完成";
            }
        }
    测试效果：
        启动端口为8090和8091两个服务，先访问8090，再访问8091.观察控制台：
        可以看到8090进入方法，并获得了锁。8091进入了方法，在8090释放锁后，才获得了锁。（观察时间节点：2022-03-26 22:42:23）        
      
      8090端口服务：
        2022-03-26 22:42:12.798  INFO 28356 --- [nio-8090-exec-1] c.h.d.api.ZookeeperController            : 进入了方法~
        2022-03-26 22:42:12.940  INFO 28356 --- [nio-8090-exec-1] c.h.d.api.ZookeeperController            : 获得了锁
        2022-03-26 22:42:23.175  INFO 28356 --- [nio-8090-exec-1] com.hlkj.distributezklock.lock.ZkLock    : 释放了锁~
        2022-03-26 22:42:23.175  INFO 28356 --- [nio-8090-exec-1] c.h.d.api.ZookeeperController            : 方法执行完成  
      8091端口服务：
        2022-03-26 22:42:14.212  INFO 25780 --- [nio-8091-exec-3] c.h.d.api.ZookeeperController            : 进入了方法~
        2022-03-26 22:42:23.045  INFO 25780 --- [nio-8091-exec-3] c.h.d.api.ZookeeperController            : 获得了锁
        2022-03-26 22:42:33.283  INFO 25780 --- [nio-8091-exec-3] com.hlkj.distributezklock.lock.ZkLock    : 释放了锁~
        2022-03-26 22:42:33.283  INFO 25780 --- [nio-8091-exec-3] c.h.d.api.ZookeeperController            : 方法执行完成  
        
### 方式2、使用curator客户端提供的锁实现      
    1、引入curator客户端
    2、直接调用curator实现的锁  