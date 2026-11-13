package com.wild.corp.adhesion.shop.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

@Component
public class DatabaseOrderNumberGenerator implements OrderNumberGenerator {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public DatabaseOrderNumberGenerator(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    DatabaseOrderNumberGenerator(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public String nextOrderNumber() {
        Long sequence = jdbcTemplate.queryForObject("select nextval('shop_order_number_seq')", Long.class);
        if (sequence == null) {
            throw new IllegalStateException("La séquence des commandes n'a retourné aucune valeur");
        }
        return "CMD-%d-%06d".formatted(Year.now(clock).getValue(), sequence);
    }
}
