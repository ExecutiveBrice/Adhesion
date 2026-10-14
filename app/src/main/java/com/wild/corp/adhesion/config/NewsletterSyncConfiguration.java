package com.wild.corp.adhesion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class NewsletterSyncConfiguration {
    @Bean(name = "newsletterSyncExecutor", destroyMethod = "shutdown")
    public ExecutorService newsletterSyncExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
