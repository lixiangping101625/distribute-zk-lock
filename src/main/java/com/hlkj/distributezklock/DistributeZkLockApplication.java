package com.hlkj.distributezklock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DistributeZkLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributeZkLockApplication.class, args);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public CuratorFramework getCuratorClient(){
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("127.0.0.1:2181", retryPolicy);
        return client;
    }
}
