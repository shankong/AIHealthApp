package com.shankong.backend.service.convert;

import com.shankong.backend.DTO.response.UserLoginResponseDTO;
import com.shankong.backend.entity.User;

public class UserConvert {
    // 构建响应DTO
    public static UserLoginResponseDTO.UserDetailResponseDTO entityToDetailResponse(User user) {
        return UserLoginResponseDTO.UserDetailResponseDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .avatar(user.getAvatar())
            .phone(user.getPhone())
            .gender(user.getGender())
            .genderDisplayName(getGenderDisplayName(user.getGender()))
            .birthday(user.getBirthday())
            .userType(user.getUserType())
            .userTypeDisplayName(user.getUserTypeDisplayName())
            .status(user.getStatus())
            .statusDisplayName(user.getStatusDisplayName())
            .displayName(user.getDisplayName())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }

    public static UserLoginResponseDTO entityToLoginResponse(String token, UserLoginResponseDTO.UserDetailResponseDTO userInfo) {
        return UserLoginResponseDTO.builder()
            .token(token)
            .roleType(userInfo.getUserType().toString())
            .userInfo(userInfo)
            .build();
    }

    /**
     * 获取性别显示名称
     * @param gender 性别代码
     * @return 性别显示名称
     */
    private static String getGenderDisplayName(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 1:
                return "男";
            case 2:
                return "女";
            default:
                return "未知";
        }
    }
}
