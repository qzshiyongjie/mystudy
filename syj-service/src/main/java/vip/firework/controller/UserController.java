package vip.firework.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import vip.firework.bean.Message;
import vip.firework.service.UserService;
import vip.firework.util.RedisUtil;

@Controller
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private UserService userService;
    @Value("${person.userName}")
    private String userName;
    @Value("${person.password}")
    private String password;
    @ResponseBody
    @RequestMapping(value = "/addUser/{userName}/{password}", produces = {"application/json;charset=UTF-8"})
    public Message addUser(@PathVariable("userName") String userName, @PathVariable("password") String password){
        log.info("addUser username:{} ,password:{}",userName,password);
        if(RedisUtil.exits(userName)){
            return Message.defaultError("用户已经存在");
        }else {
            RedisUtil.set(userName,password);
        }
        return userService.addUser(userName,password);
    }
    @RequestMapping(value = "/login/{userName}/{password}", produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public Message login(@PathVariable("userName") String userName, @PathVariable("password") String password){
        log.info("login username:{} ,password:{}",userName,password);
        if(!this.userName.equals(userName)){
            return Message.defaultError("用户名不对");
        }else if(!this.password.equals(password)) {
            return Message.defaultError("密码不正确");
        }
        return Message.defaultSuccess("登入成功");
    }
}
