package com.chat.socket;

import com.alibaba.fastjson.JSONObject;
import com.chat.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * websocket服务
 *
 * @author peppazhang
 */
@ServerEndpoint("/imserver/{userId}")
@Component
@Slf4j
public class WebSocketServer {
    /**
     * 线程安全的hashmap，用来存放每个客户端的MyWebSocket对象
     */
    public static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 在线用户的数量，应该设计为线程安全的
     */
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    /**
     * 于某个客户端的连接会话，需要通过它来发送消息
     */
    private Session session;

    /**
     * 接收userId
     */
    private String userId;


    /**
     * 连接建立成功的调用方法
     *
     * @param session
     * @param userId
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        this.session = session;
        this.userId = userId;

        //连接存在
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
        } else {
            webSocketMap.put(userId, this);
            addOnlineCount();
        }

        log.info("用户连接数: " + userId + ",当前在线人数为: " + this.getOnlineCount());
    }


    /**
     * 连接关闭调用方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            subOnlineCount();
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("msgType", Constant.MSG_TYPE_OFFLINE);
                jsonObject.put("userName", userId);
                //群发用户在线信息
                sendGroupChatMsg(jsonObject);

                //群发消息，下线问候
                toGreet(Constant.MSG_TYPE_OFFLINE);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        log.info("用户退出: " + userId + " ,当前在线人数: " + this.getOnlineCount());
    }

    /**
     * 收到客户端消息调用方法
     *
     * @param message 消息体
     * @param session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info(userId + "用户的消息,报文: " + message);
        if (StringUtils.isNotBlank(message)) {
            try {
                //解析消息
                JSONObject jsonObject = JSONObject.parseObject(message);

                //消息类型
                String msgType = jsonObject.getString("msgType");
                if (!msgType.isEmpty()) {
                    //追加发送人信息
                    jsonObject.put("fromUserId", userId);
                    //追加发送时间
                    jsonObject.put("sendTime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .format(LocalDateTime.now()));
                    //消息类型为上线
                    if (Constant.MSG_TYPE_ONLINE.equals(msgType)) {
                        //群发给所有在线人员当前在线信息
                        List<String> onlineUsers = this.getOnlineUsers();
                        //放入所有在线用户并群发
                        jsonObject.put("onlineUsers", onlineUsers);
                        sendGroupChatMsg(jsonObject);
                        //群发消息，上线问候
                        toGreet(Constant.MSG_TYPE_ONLINE);

                        //消息类型为下线
                    } else if (Constant.MSG_TYPE_OFFLINE.equals(msgType)) {
                        //群发给所有人某人下线了
                        sendGroupChatMsg(jsonObject);

                        //消息类型为群聊
                    } else if (Constant.MSG_TYPE_GROUP_CHAT.equals(msgType)) {
                        //群发给所有在线的人消息
                        sendGroupChatMsg(jsonObject);

                        //单聊
                    } else if (Constant.MSG_TYPE_SINGLE_CHAT.equals(msgType)) {
                        //消息一对一发送
                        sendSingleChatMsg(jsonObject);

                    } else if (Constant.MSG_TYPE_HEART_CHECK.equals(msgType)) {
                        sendHeartCheck(jsonObject);
                    } else {
                        log.error("所发送的消息类型没有或未定义!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendHeartCheck(JSONObject jsonObject) {
        jsonObject.put("toUserId", this.userId);
        sendSingleChatMsg(jsonObject);
    }

    /**
     * 上下线问候
     */
    private void toGreet(String msgType) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msgType", Constant.MSG_TYPE_GROUP_CHAT);
        jsonObject.put("userName", userId);
        jsonObject.put("sendTime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(LocalDateTime.now()));
        String greetMsg = "";

        if (Constant.MSG_TYPE_ONLINE.equals(msgType)) {
            greetMsg = "hello,我上线了！！！";
        }
        if (Constant.MSG_TYPE_OFFLINE.equals(msgType)) {
            greetMsg = "我溜了！！！";
        }
        jsonObject.put("contentText", greetMsg);
        sendGroupChatMsg(jsonObject);
    }

    /**
     * 消息群发
     *
     * @param jsonObject
     */
    private void sendGroupChatMsg(JSONObject jsonObject) {
        //迭代在线人员map
        Iterator<Map.Entry<String, WebSocketServer>> iterator = webSocketMap.entrySet().iterator();
        try {
            while (iterator.hasNext()) {
                Map.Entry<String, WebSocketServer> entry = iterator.next();
                webSocketMap.get(entry.getKey()).sendMessage(jsonObject.toJSONString());
                //TODO 是否需要将消息存储
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 单聊消息发送
     *
     * @param jsonObject
     */
    private void sendSingleChatMsg(JSONObject jsonObject) {
        //消息接收人
        String toUserId = jsonObject.getString("toUserId");

        //消息体
        String msgBody = jsonObject.toJSONString();

        //传送给对应toUserId用户的webSocket
        if (StringUtils.isNotBlank(toUserId) && webSocketMap.containsKey(userId)) {
            webSocketMap.get(toUserId).sendMessage(msgBody);
        } else {
            log.error("所发送的用户: " + userId + " 不在线!!!");
            //不在，则存到数据库或者redis
        }
    }

    /**
     * 异常时方法
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误：" + this.userId + " ,原因:" + error.getMessage());
        error.printStackTrace();
    }


    /**
     * 消息发送
     */
    private void sendMessage(String message) {
        try {
            this.session.getBasicRemote().sendText(message);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 得到所有在线用户
     *
     * @return
     */
    private List<String> getOnlineUsers() {
        List<String> onlineUsers = new ArrayList<>();
        Iterator<Map.Entry<String, WebSocketServer>> iterator = webSocketMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WebSocketServer> entry = iterator.next();
            onlineUsers.add(entry.getKey());
        }
        return onlineUsers;
    }

    /**
     * 在线人数+1
     */
    private void addOnlineCount() {
        WebSocketServer.onlineCount.getAndIncrement();
    }

    /**
     * 获取在线人数
     *
     * @return
     */
    private Integer getOnlineCount() {
        return onlineCount.get();
    }

    /**
     * 在线人数-1
     */
    private void subOnlineCount() {
        WebSocketServer.onlineCount.getAndDecrement();
    }
}
