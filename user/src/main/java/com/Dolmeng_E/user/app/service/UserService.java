package com.Dolmeng_E.user.app.service;

import com.Dolmeng_E.user.app.domain.User;
import com.Dolmeng_E.user.app.dto.UserCreateResDto;
import com.Dolmeng_E.user.app.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public void create(UserCreateResDto dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()) throw new EntityExistsException("중복되는 이메일입니다.");

        // todo - 비밀번호 암호화, 프로필 이미지 저장 후 url 가져오기

        User user = dto.toEntity(dto.getPassword(), "url");
        userRepository.save(user);
    }

}
