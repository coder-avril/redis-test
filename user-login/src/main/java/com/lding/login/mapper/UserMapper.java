package com.lding.login.mapper;

import com.lding.login.Dto.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User getUser(User user) {
        if (user.getUsername().startsWith("admin")) {
            return user;
        } else {
            return null;
        }
    }
}
