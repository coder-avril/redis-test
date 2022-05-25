package com.lding.login.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lding.login.Dto.User;
import com.lding.login.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.concurrent.TimeUnit;

@Service
public class UserInfoService {
    @Autowired
    private UserMapper mapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean check(User user) throws Exception {
        return getByCache(user) != null;
    }

    private User getByCache(User user) throws JsonProcessingException {
        System.out.println("从redis缓存中查询查询结果: " + user.getUsername());
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String value = ops.get("user:" + user.getUsername());//直接从本地内存(比较短的时间1min)查询, 对于redis的压力会小
        ObjectMapper objectMapper = new ObjectMapper();
        User userInfo= null;
        //判断redis的返回结果
        if(StringUtils.isEmpty(value)){
            // 查询数据库
            userInfo = getByDb(user);
            if(userInfo != null){ //添加到缓存中
                ops.set("user:" + user.getUsername(), objectMapper.writeValueAsString(userInfo),7, TimeUnit.DAYS);
            } else  {
                ops.set("user:" + user, objectMapper.writeValueAsString(new User()),1, TimeUnit.MINUTES);
            }
        }else{
            userInfo = objectMapper.readValue(value,User.class);
        }
        return userInfo;
    }

    private User getByDb(User user) {
        System.out.println("从数据库查询结果: " + user.getUsername());
        return mapper.getUser(user);
    }
}
