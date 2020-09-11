package com.meridian.platform.txnposting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TxnPostingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TxnPostingServiceApplication.class, args);
    }
}
