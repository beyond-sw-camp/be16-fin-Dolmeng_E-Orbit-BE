package com.Dolmeng_E.chat_db.openVidu.controller;

import com.Dolmeng_E.chat_db.openVidu.service.OpenViduChatService;
import com.Dolmeng_E.chat_db.openVidu.service.OpenViduService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-vidu")
@RequiredArgsConstructor
public class OpenViduController {
    private final OpenViduService openViduService;
    private final OpenViduChatService openViduChatService;

    // 특정 채팅방(roomId)에 대한 OpenVidu 토큰 요청
    @PostMapping("/room/{roomId}/openvidu/token")
    public ResponseEntity<String> getOpenViduToken(@PathVariable Long roomId) {
        try {
            String token = openViduChatService.getOpenViduToken(roomId);
            return new ResponseEntity<>(token, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // 참여자가 아닌 경우 등 비즈니스 로직 오류 처리
            return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            // OpenVidu 서버 통신 오류 등 기타 오류 처리
            return new ResponseEntity<>("토큰 발급 중 오류 발생: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    // openVidu 녹화 시작
//    @PostMapping("/room/{roomId}/openvidu/recordings/start")
//    public ResponseEntity<?> startRecording(@PathVariable Long roomId) {
//        try {
//            // Long 타입의 roomId를 OpenVidu Service가 요구하는 String 타입의 sessionId로 변환
//            String sessionId = String.valueOf(roomId);
//
//            // OpenVidu Service를 통해 녹화 시작 요청
//            Recording recording = openViduService.startRecording(sessionId);
//            return new ResponseEntity<>(recording, HttpStatus.OK);
//        } catch (IllegalStateException e) {
//            // OpenViduService에서 Active Session이 없을 때 던지는 예외를 NOT_FOUND로 처리
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND); // 404
//        } catch (OpenViduJavaClientException e) {
//            // OpenVidu Java Client의 오류
//            return new ResponseEntity<>("OpenVidu 클라이언트 오류: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch (OpenViduHttpException e) {
//            // OpenVidu 서버의 HTTP 오류 (400, 501 등)를 BAD_REQUEST로 클라이언트에 전달
//            return new ResponseEntity<>("OpenVidu HTTP 오류: " + e.getMessage(), HttpStatus.BAD_REQUEST);
//        } catch (Exception e) {
//            // 기타 예상치 못한 오류
//            return new ResponseEntity<>("녹화 시작 중 알 수 없는 오류 발생: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    // openVidu 녹화 중지
//    @PostMapping("/room/{roomId}/openvidu/recordings/stop/{recordingId}")
//    public ResponseEntity<?> stopRecording(@PathVariable String recordingId) {
//        try {
//            Recording recording = openViduService.stopRecording(recordingId);
//            return new ResponseEntity<>(recording, HttpStatus.OK);
//        } catch (OpenViduJavaClientException e) {
//            return new ResponseEntity<>("OpenVidu 클라이언트 오류: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch (OpenViduHttpException e) {
//            return new ResponseEntity<>("OpenVidu HTTP 오류: " + e.getMessage(), HttpStatus.BAD_REQUEST);
//        } catch (Exception e) {
//            return new ResponseEntity<>("녹화 중지 중 알 수 없는 오류 발생: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
}