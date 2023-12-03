package com.mq.myqq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mq.myqq.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {

    @Insert("insert into user(nickname, password) values(#{nickname}, #{password})")
    int signup(String nickname, String password);

    @Select("select * from user where nickname = #{nickname} and password = #{password}")
    User login(String nickname, String password);
}
