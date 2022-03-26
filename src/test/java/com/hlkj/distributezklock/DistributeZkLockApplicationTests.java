package com.hlkj.distributezklock;

import com.hlkj.distributezklock.lock.ZkLock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

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

}
