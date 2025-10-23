package com.Dolmeng_E.drive.common.service;

import com.Dolmeng_E.drive.common.dto.EditorBatchMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class EditorWebSocketHandler extends TextWebSocketHandler {
    // JSON 처리를 위한 ObjectMapper
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 문서 ID별로 WebSocket 세션들을 관리하는 맵
    // ConcurrentHashMap: 동시성 이슈를 방지하기 위해 사용
    // CopyOnWriteArraySet: 세션을 순회하면서 요소를 제거할 때 발생할 수 있는 동시성 이슈를 방지
    private final Map<String, Set<WebSocketSession>> documentSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("세션 연결됨: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        EditorBatchMessageDto editorMessage = objectMapper.readValue(payload, EditorBatchMessageDto.class);

        String documentId = editorMessage.getDocumentId();

        // documentId를 키로 사용하여 세션 집합을 가져오거나, 없으면 새로 생성
        Set<WebSocketSession> sessions = documentSessions.computeIfAbsent(documentId, k -> new CopyOnWriteArraySet<>());

//        if (editorMessage.getType() == EditorMessageDto.MessageType.JOIN) {
//            // JOIN 메시지인 경우, 현재 세션을 해당 문서의 세션 목록에 추가
//            sessions.add(session);
//            System.out.println("세션 " + session.getId() + "이(가) 문서 " + documentId + "에 참여했습니다.");
//        } else if (editorMessage.getType() == EditorMessageDto.MessageType.UPDATE) {
//            // UPDATE 메시지인 경우, 해당 문서의 다른 모든 세션에 메시지 브로드캐스팅
//            broadcast(documentId, session, message);
//        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("세션 연결 끊김: " + session.getId());
        // 모든 문서 세션 맵을 순회하며 현재 세션을 제거
        documentSessions.values().forEach(sessions -> sessions.remove(session));
    }

    // 메시지를 보낸 세션을 제외한 다른 모든 세션에게 메시지를 전송하는 메서드
    private void broadcast(String documentId, WebSocketSession senderSession, TextMessage message) throws IOException {
        Set<WebSocketSession> sessions = documentSessions.get(documentId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                // 메시지를 보낸 클라이언트를 제외하고, 연결이 열려있는 다른 클라이언트에게만 전송
                if (session.isOpen() && !session.getId().equals(senderSession.getId())) {
                    session.sendMessage(message);
                }
            }
        }
    }
}
