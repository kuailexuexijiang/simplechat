package com.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面跳转控制器类
 * @author zhangxin
 */
@Controller
public class IndexController {

    @GetMapping(value = "/index")
    public String index(){
        return "index";
    }
}
