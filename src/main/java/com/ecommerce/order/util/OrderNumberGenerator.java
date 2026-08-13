package com.ecommerce.order.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicLong sequence = new AtomicLong(0);

    public String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        long seq = sequence.incrementAndGet();
        return String.format("ORD%s%04d", timestamp, seq % 10000);
    }
}