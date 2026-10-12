package com.shankong.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shankong.backend.entity.User;

@Mapper 
public interface UserMapper extends BaseMapper<User> {

}
