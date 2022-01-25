package com.chat.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;

/**
 * 页面跳转控制器类
 * @author zhangxin
 */
@Controller
@Slf4j
public class IndexController {

    @GetMapping(value = "/index")
    public String index(ModelMap modelMap){
        try {
            modelMap.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return "index";
    }
}
