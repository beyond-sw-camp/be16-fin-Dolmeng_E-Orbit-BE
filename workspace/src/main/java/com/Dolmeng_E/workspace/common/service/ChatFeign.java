package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.*;
import com.example.modulecommon.dto.CommonSuccessDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "chat-service")
public interface ChatFeign {
    @GetMapping("/chat/room/{roomId}/unread-messages")
    ResponseEntity<CommonSuccessDto> getUnreadMessageListByRoom (@PathVariable("roomId") Long roomId, @RequestHeader("X-User-Id")String userId);

    @PostMapping("/chat/room/new-user")
    ResponseEntity<CommonSuccessDto> inviteChatParticipants(
            @RequestBody ChatInviteReqDto dto
    );

    @PostMapping("/chat/room")
    ResponseEntity<CommonSuccessDto> createChatRoom(
            @RequestBody ChatCreateReqDto dto
    );
}
