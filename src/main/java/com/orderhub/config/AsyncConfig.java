package com.orderhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Bật tính năng xử lý bất đồng bộ
public class AsyncConfig {

    @Bean(name = "orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số luồng cơ bản duy trì trong pool
        executor.setCorePoolSize(5);
        // Số luồng tối đa khi hàng đợi đầy
        executor.setMaxPoolSize(10);
        // Dung lượng hàng đợi chứa các tác vụ chờ xử lý
        executor.setQueueCapacity(100);
        // Tiền tố định danh thread để dễ soi log
        executor.setThreadNamePrefix("OrderAsync-");
        executor.initialize();
        return executor;
    }
}