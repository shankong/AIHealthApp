package com.shankong.backend.service;

import com.shankong.backend.DTO.response.UserLoginResponseDTO;
import com.shankong.backend.util.JWTTokenUtil;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.shankong.backend.exception.BusinessException;

import com.shankong.backend.common.Result;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shankong.backend.DTO.command.UserLoginCommandDTO;

import com.shankong.backend.entity.User;
import com.shankong.backend.mapper.UserMapper;
import com.shankong.backend.service.convert.UserConvert;

import jakarta.annotation.Resource;


@Service 
public class UserService {
    @Resource 
    private UserMapper userMapper;

    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, commandDTO.getUsername())
            .or()
            .eq(User::getEmail, commandDTO.getUsername());

        // 调用MybatisPlus根据用户名或邮箱查询用户
        User user = userMapper.selectOne(queryWrapper);
        //System.out.println(user);

        // 检查用户是否存在
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查密码是否正确
        String inputPassward = commandDTO.getPassword();
        if(!bCryptPasswordEncoder.matches(inputPassward, user.getPassword())){
            throw new BusinessException("密码错误");
        }

        // 检查用户状态
        if(!user.isActive()){
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        // 生成token
        String token = JWTTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
        //System.out.println(token);

        UserLoginResponseDTO userInfo = UserConvert.entityToLoginResponse(token, UserConvert.entityToDetailResponse(user));

        return userInfo;
    }
}
