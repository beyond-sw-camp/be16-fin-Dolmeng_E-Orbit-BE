package com.Dolmeng_E.chat_db;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {
		"com.Dolmeng_E.chat_db",
        "com.example.modulecommon"
})
public class ChatDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatDbApplication.class, args);
	}

}
