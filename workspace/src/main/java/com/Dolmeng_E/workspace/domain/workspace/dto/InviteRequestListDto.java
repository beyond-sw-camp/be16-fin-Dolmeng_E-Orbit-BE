package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InviteRequestListDto {
    private List<String> emailList;
}