package com.Dolmeng_E.workspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@EnableFeignClients
@SpringBootApplication
@ComponentScan(basePackages = {
		"com.Dolmeng_E.workspace", // 여기는 서비스 명에 맞게 user -> drive등 바꿔주기
		"com.example.modulecommon"
})
public class WorkspaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkspaceApplication.class, args);
	}

}
