package com.hlkj.distributezklock.api;

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
