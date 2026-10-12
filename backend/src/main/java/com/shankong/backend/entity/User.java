package com.shankong.backend.entity;

import lombok.Builder;
import com.shankong.backend.enumClass.UserType;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.shankong.backend.enumClass.UserStatus;

// 用户实体类
@Data 
@TableName("user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // 用户ID：32 位 UUID 字符串（不带横线）
    // ASSIGN_UUID = 插入时由 MyBatis-Plus 自动生成，等价于 IdWorker.get32UUID()，
    // 所以数据库列是 VARCHAR(32)，不再是自增 BIGINT。
    // 注意两种 UUID 格式别混用：这里生成的是 32 位纯十六进制，
    // 而 UUID.randomUUID().toString() 是 36 位带横线的。
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    // 用户名
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 100, message = "用户名长度必须在4到100之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    // 手机号
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    // 密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    private String password;

    // 昵称
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    // 头像
    @Size(max = 255, message = "头像路径长度不能超过255个字符")
    private String avatar;

    // 性别
    private Integer gender;

    // 生日
    private LocalDate birthday;

    // 用户类型 1:普通用户 2:管理员
    @TableField("user_type")
    private Integer userType;

    // 用户状态 1:正常 2:禁用
    private Integer status;

    // 创建时间
    @TableField("created_at")
    private LocalDateTime createdAt;

    // 更新时间
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 是否是普通用户
     */
    public boolean isNormalUser() {
        return UserType.USER.getCode().equals(this.userType);
    }

    /**
     * 是否为正常状态
     */
    public boolean isActive() {
        return UserStatus.NORMAL.getCode().equals(this.status);
    }

    /**
     * 是否被禁用
     */
    public boolean isDisabled() {
        return UserStatus.DISABLED.getCode().equals(this.status);
    }

    /**
     * 获取显示名称（优先显示昵称，否则显示用户名）
     */
    public String getDisplayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname : username;
    }

    /**
     * 获取用户类型显示名称
     */
    public String getUserTypeDisplayName() {
        try {
            return UserType.fromCode(userType).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    /**
     * 获取用户状态显示名称
     */
    public String getStatusDisplayName() {
        try {
            return UserStatus.fromCode(status).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
