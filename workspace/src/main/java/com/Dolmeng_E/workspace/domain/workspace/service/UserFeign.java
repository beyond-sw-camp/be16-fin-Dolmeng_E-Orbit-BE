package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.domain.workspace.dto.UserInfoResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserFeign {

    @GetMapping("/user/return")
    UserInfoResDto fetchUserInfo(@RequestHeader("X-User-Email")String userEmail);
}
