package com.hlkj.distributezklock;

import com.hlkj.distributezklock.lock.ZkLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class DistributeZkLockApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void testZkLock() throws Exception {
        ZkLock zkLock = new ZkLock();
        boolean b = zkLock.getLock("order");
        log.info("获取锁的结果：" + b);
        zkLock.close();
    }

    @Test
    public void testCuratorLock() {
        //1、创建连接
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", retryPolicy);
        client.start();
        //2、使用锁
        //lockPath区分不同的业务
        String lockPath = "/order";
        InterProcessMutex lock = new InterProcessMutex(client, lockPath);
        try {
            if ( lock.acquire(30, TimeUnit.SECONDS) )
            {
                try
                {
                    // do some work inside of the critical section here
                    log.info("获得了锁");
                }
                finally
                {
                    lock.release();
                }
            }
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
