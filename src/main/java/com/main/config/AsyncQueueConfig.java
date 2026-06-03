package com.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
@EnableAsync
public class AsyncQueueConfig {

    @Bean(name = "CamdxQueueExecutor")
    public Executor ekycQueueExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // 5 workers making active network connection requests simultaneously
        executor.setMaxPoolSize(10);       // Scales up to 10 workers if capacity overloads
        executor.setQueueCapacity(2000);   // Accumulates up to 2,000 requests waiting in RAM
        executor.setThreadNamePrefix("CamDX-Worker-");
        executor.initialize();
        return executor;
    }
}