package com.chat.config;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket消息通讯配置类
 * 开启WebSocket支持
 * @author zhangxin
 */
@Configurable
@Component
public class WebSocketConfig {

    @Bean
    ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }
}
