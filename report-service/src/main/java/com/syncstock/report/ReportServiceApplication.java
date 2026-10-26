package com.syncstock.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.syncstock.report", "com.syncstock.common"})
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
