package com.Dolmeng_E.chat_db.openVidu.service;

import io.openvidu.java.client.*;
import org.springframework.stereotype.Service;

@Service

public class OpenViduService {
    private final OpenVidu openVidu;

    public OpenViduService(OpenVidu openVidu) {
        this.openVidu = openVidu;
    }

    public String createSessionAndGetToken(String customSessionId, String connectionRole)
            throws OpenViduJavaClientException, OpenViduHttpException {

        // SessionProperties 설정 (세션이 없다면 새로 생성할 때 사용)
        SessionProperties sessionProperties = new SessionProperties.Builder()
                .customSessionId(customSessionId) // 채팅방 ID 등을 세션 ID로 사용
                .build();

        Session session;

        // 2. OpenVidu 서버에서 세션 생성 또는 검색.
        this.openVidu.fetch();
        Session existingSession = this.openVidu.getActiveSession(customSessionId);

        if (existingSession == null) {
            // 세션이 존재하지 않으면, 새로운 세션을 생성.
            session = this.openVidu.createSession(sessionProperties);
        } else {
            // 세션이 이미 존재하면, 기존 세션 사용.
            session = existingSession;
        }

        // ConnectionProperties 설정 (토큰 발급 설정)
        ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                .role(io.openvidu.java.client.OpenViduRole.valueOf(connectionRole))
                .build();

        // 토큰(Connection) 생성
        Connection connection = session.createConnection(connectionProperties);

        // 토큰 반환
        return connection.getToken();
    }

    /*
    // openVidu 녹화 시작 요청
    public Recording startRecording(String sessionId)
            throws OpenViduJavaClientException, OpenViduHttpException {

        // 1. OpenVidu 서버에 존재하는 활성 세션인지 확인
        openVidu.fetch(); // 서버의 최신 세션 상태를 가져옵니다.
        Session session = openVidu.getActiveSession(sessionId);

        if (session == null) {
            // 만약 서버에서 세션을 못찾으면, 오류 메시지를 좀 더 명확하게 던집니다.
            throw new IllegalStateException("Active session not found in OpenVidu Server for ID: " + sessionId);
        }

        // 2. 녹화 속성 설정
        RecordingProperties properties = new RecordingProperties.Builder()
                .outputMode(Recording.OutputMode.COMPOSED) // 기본적으로 COMPOSED 모드 사용
                .resolution("1280x720") // 녹화 해상도 설정 (선택 사항)
                .build();

        // 3. 녹화 시작 요청 및 OpenVidu 서버의 응답 (Recording 객체) 반환
        return openVidu.startRecording(sessionId, properties);
    }

    // openVidu 녹화 중지 요청
    public Recording stopRecording(String recordingId)
            throws OpenViduJavaClientException, OpenViduHttpException {
        // OpenVidu 서버에 녹화 중지 요청을 보내고, 중지된 Recording 객체를 반환합니다.
        return openVidu.stopRecording(recordingId);
    }
    */
}
