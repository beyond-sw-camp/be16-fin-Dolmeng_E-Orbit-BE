package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service")
public interface UserFeign {

    @GetMapping("/user/return/by-id")
    UserInfoResDto fetchUserInfoById(@RequestHeader("X-User-Id")String userId);

    @PostMapping("/user/return/users")
    UserInfoListResDto fetchUserListInfo(@RequestBody UserIdListDto userIdListDto);
}
