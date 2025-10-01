package com.Dolmeng_E.workspace.domain.user_group.service;

import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserGroupService {
    private final UserGroupRepository userGroupRepository;
}
