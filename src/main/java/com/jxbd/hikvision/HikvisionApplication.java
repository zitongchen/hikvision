package com.jxbd.hikvision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HikvisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(HikvisionApplication.class, args);
        System.out.println("海康威视布防撤防首页：http://localhost:50000/hikvision");
    }

}
