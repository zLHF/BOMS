package com.boms.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private Long tenantId;
    private String username;
    private String realName;
    private List<String> permissions;
}
