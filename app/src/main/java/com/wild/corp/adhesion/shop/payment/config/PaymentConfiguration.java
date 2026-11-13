package com.wild.corp.adhesion.shop.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.wild.corp.adhesion.shop.payment.helloasso.HelloAssoPaymentProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PaymentProperties.class, HelloAssoPaymentProperties.class})
public class PaymentConfiguration {
}
