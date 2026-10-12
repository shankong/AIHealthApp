package com.shankong.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import com.shankong.backend.common.Result;
import com.shankong.backend.DTO.command.UserLoginCommandDTO;
import com.shankong.backend.DTO.response.UserLoginResponseDTO;

import org.springframework.web.bind.annotation.RequestBody;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.shankong.backend.service.UserService;

@RestController 
@RequestMapping("/api/user")
public class User {
    @Resource 
    private UserService userService;

    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {
        UserLoginResponseDTO result = userService.login(commandDTO);
        return Result.success(result);
    }
}
