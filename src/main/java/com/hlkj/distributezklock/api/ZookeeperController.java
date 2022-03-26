package com.hlkj.distributezklock.api;

import com.hlkj.distributezklock.lock.ZkLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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


    @Autowired
    private CuratorFramework client;
    @GetMapping("/curatorLock")
    public String curatorLock() {
        log.info("进入了方法~");
        //lockPath区分不同的业务
        String lockPath = "/order";
        InterProcessMutex lock = new InterProcessMutex(client, lockPath);
        try {
            if (lock.acquire(30, TimeUnit.SECONDS) )
            {
                log.info("获得了锁");
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                lock.release();
                log.info("释放了锁");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("方法执行完成");
        return "方法执行完成";
    }
}
