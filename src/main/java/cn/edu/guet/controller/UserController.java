package cn.edu.guet.controller;

import cn.edu.guet.bean.User;
import cn.edu.guet.mvc.annotation.Controller;
import cn.edu.guet.mvc.annotation.RequestMapping;

@Controller
public class UserController {

    @RequestMapping("user/addUser.do")
    public String  addUser(User user){
        System.out.println("添加用户");
         return "forward:viewUser.jsp";
    }

    @RequestMapping("user/viewUser.do")
    public User  viewUser(){
        System.out.println("查看用户");
        User user=new User();
        user.setUsername("nnnnn");
        user.setPassword("haha1234");
        user.setAge(33);
        return user;
    }
}
