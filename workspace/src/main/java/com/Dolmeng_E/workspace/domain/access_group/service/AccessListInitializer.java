package com.Dolmeng_E.workspace.domain.access_group.service;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessList;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessType;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessListInitializer implements CommandLineRunner {

    private final AccessListRepository accessListRepository;

    @Override
    public void run(String... args) {
        // AccessType enum 에 정의된 값들을 모두 순회
        for (AccessType type : AccessType.values()) {
            // DB 에 이미 같은 accessType 이 있는지 검사
            accessListRepository.findByAccessType(type).orElseGet(() ->
                    // 없다면 새로 insert
                    accessListRepository.save(
                            AccessList.builder()
                                    .accessName(type.name())           // 권한 이름 (ex: INVITE_USER)
                                    .description(type.name() + " 권한") // 권한 설명
                                    .accessType(type)                  // Enum 값 그대로 저장
                                    .build()
                    )
            );
        }
    }
}

