package com.Dolmeng_E.chat_db.openVidu.service;

import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class OpenViduChatService {
    private final OpenViduService openViduService;

    public String getOpenViduToken(Long roomId){
        // 1. 해당 방에 현재 로그인한 사용자가 참여자인지 검증 (기존 로직 재사용)
//        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(()-> new EntityNotFoundException("room cannot be found"));
//        Member member = memberRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()->new EntityNotFoundException("member cannot be found"));
//
//        if (!isRoomPaticipant(member.getEmail(), roomId)) {
//            throw new IllegalArgumentException("본인이 속하지 않은 채팅방입니다. 토큰 발급 불가.");
//        }

        // 2. 채팅방 ID를 OpenVidu Custom Session ID로 사용
        String customSessionId = String.valueOf(roomId);
        String role = "PUBLISHER"; // 모든 참여자에게 영상/음성 송수신 권한 부여

        try {
            // 3. OpenViduService를 호출하여 세션을 생성/가져오고 토큰 발급
            return openViduService.createSessionAndGetToken(customSessionId, role);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            // OpenVidu 서버 통신 오류 처리
            e.printStackTrace();
            throw new RuntimeException("OpenVidu 토큰 발급 중 오류 발생: " + e.getMessage());
        }
    }
}