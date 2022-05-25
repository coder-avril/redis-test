package com.lding.login.controller;

import com.lding.login.Dto.PageResult;
import com.lding.login.Dto.User;
import com.lding.login.service.UserInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;

@RestController
public class UserController {
    @Resource
    private UserInfoService userService;

    @GetMapping("/login")
    public PageResult login(User user) throws Exception {
        if(userService.check(user)){
            return PageResult.success();
        }else{
            return PageResult.mark("用户名或者密码错误");
        }
    }
}
