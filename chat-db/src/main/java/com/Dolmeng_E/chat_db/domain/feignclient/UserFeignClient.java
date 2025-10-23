package com.Dolmeng_E.chat_db.domain.feignclient;

import com.Dolmeng_E.chat_db.common.dto.UserInfoResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// name 부분은 eureka에 등록된 application.name을 의미
@FeignClient(name = "user-service")
public interface UserFeignClient {
    @GetMapping("/user/return")
    UserInfoResDto fetchUserInfo(@RequestHeader("X-User-Email")String userEmail);
    @GetMapping("/user/return/by-id")
    UserInfoResDto fetchUserInfoById(@RequestHeader("X-User-Id")String userId);
}
