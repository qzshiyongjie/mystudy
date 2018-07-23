package vip.firework.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vip.firework.bean.Message;
import vip.firework.mapper.UserMapper;
import vip.firework.model.User;
import vip.firework.service.UserService;

@Service("userServiceImpl")
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    public Message addUser(String userName, String password) {
        User user = new User();
        user.setName(userName);
        user.setPassword(password);
        userMapper.insertSelective(user);
        return Message.defaultSuccess(user);
    }
}
