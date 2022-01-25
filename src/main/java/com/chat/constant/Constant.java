package com.chat.constant;

import org.springframework.stereotype.Component;

/**
 * 常量类
 * @author zhangxin
 */
@Component
public class Constant {
    /**
     * 群聊
     */
    public static final String MSG_TYPE_GROUP_CHAT = "GROUP_CHAT";

    /**
     * 单聊
     */
    public static final String MSG_TYPE_SINGLE_CHAT = "SINGLE_CHAT";

    /**
     * 上线
     */
    public static final String MSG_TYPE_ONLINE = "ONLINE";

    /**
     * 下线
     */
    public static final String MSG_TYPE_OFFLINE = "OFFLINE";

    /**
     * 心跳测试
     */
    public static final String MSG_TYPE_HEART_CHECK = "HEART_CHECK";
}
