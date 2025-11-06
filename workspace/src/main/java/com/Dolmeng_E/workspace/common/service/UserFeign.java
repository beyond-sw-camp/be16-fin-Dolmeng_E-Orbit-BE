package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "user-service")
public interface UserFeign {

    @GetMapping("/user/return/by-id")
    UserInfoResDto fetchUserInfoById(@RequestHeader("X-User-Id")String userId);

    @PostMapping("/user/return/by-email")
    UserInfoListResDto fetchUserInfoByEmail(@RequestBody UserEmailListDto userEmailListDto);

    @PostMapping("/user/return/users")
    UserInfoListResDto fetchUserListInfo(@RequestBody UserIdListDto userIdListDto);

    @GetMapping("/user/return/all-users")
    UserInfoListResDto fetchAllUserListInfo(@RequestHeader("X-User-Id")String userId);

    @PostMapping("/user/not-in-workspace")
    UserInfoListResDto fetchUsersNotInWorkspace(@RequestBody UserIdListDto dto);

    @GetMapping("/shared-calendars/chatbot/schedules")
    List<SharedCalendarResDto> getSchedulesForAgent(@RequestHeader("X-User-Id") String userId, @SpringQueryMap GetSchedulesForChatBotReqDto dto);

    @GetMapping("/shared-calendars/chatbot/todos")
    List<SharedCalendarResDto> getTodosForAgent(@RequestHeader("X-User-Id") String userId, @SpringQueryMap GetSchedulesForChatBotReqDto dto);
}
