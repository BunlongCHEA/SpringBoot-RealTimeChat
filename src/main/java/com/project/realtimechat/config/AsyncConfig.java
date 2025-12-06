package com.project.realtimechat.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    @Value("${spring. task.execution.pool.core-size:5}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:10}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${spring.task. execution.thread-name-prefix:async-}")
    private String threadNamePrefix;

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

         log.info("Async executor configured - CorePool: {}, MaxPool: {}, QueueCapacity: {}", 
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * Custom exception handler for async methods
     */
    private static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Async exception in method '{}': {}", method.getName(), ex. getMessage(), ex);
        }
    }
    
}
