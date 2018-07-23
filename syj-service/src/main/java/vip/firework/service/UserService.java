package vip.firework.service;

import vip.firework.bean.Message;

public interface UserService {
    Message addUser(String userName, String password);
}
