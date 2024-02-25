package com.typicode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class TheApp {
    public static void main(String[] args) {
        System.out.println("Args = " + Arrays.toString(args));
        SpringApplication.run(TheApp.class, args);
    }
}
