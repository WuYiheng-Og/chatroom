package com.example.chatserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jdk.internal.util.xml.impl.Input;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @ClassName WebSocketConfig
 * @Description TODO
 * @Author zrc
 * @Date 11:01
 * @Version 1.0
 **/
//注册成组件
@Component
//定义websocket服务器端，它的功能主要是将目前的类定义成一个websocket服务器端。注解的值将被用于监听用户连接的终端访问URL地址
@ServerEndpoint("/websocket/{username}")
//如果不想每次都写private  final Logger logger = LoggerFactory.getLogger(当前类名.class); 可以用注解@Slf4j;可以直接调用log.info
@Slf4j
public class WebSocketController {

    //实例一个session，这个session是websocket的session
    private Session session;

    //存放当前用户名
    private String userName;

    //存放需要接受消息的用户名
    private String toUserName;

    //存放在线的用户数量
    private static Integer userNumber = 0;

    //存放websocket的集合
    private static CopyOnWriteArraySet<WebSocketController> webSocketControllerSet = new CopyOnWriteArraySet<>();

    // 存放在线用户列表名单
    private static List<String> userLists;

    //前端请求时一个websocket时
    @OnOpen
    public void onOpen(Session session,@PathParam("username") String username) throws IOException {
        // TODO 判断是否已经登录，登录的话提醒客户端已经登录（可以做，但是要先拦截，还要服务端主动关闭流（不用客户端关闭，太麻烦，但是现在还不会在服务端主动关闭））
//        for (WebSocketController webSocketController : webSocketControllerSet) {
//            if (webSocketController.userName.equals("username")){
//                session.getBasicRemote().sendText("已经登录，请勿重复登录！");
//                this.onClose();
//                return;
//            }
//        }

        this.session = session;
        //将当前对象放入webSocketSet
        webSocketControllerSet.add(this);
        //增加在线人数
        userNumber++;
        //保存当前用户名
        this.userName = username;
        //获得所有的用户
        if(userLists==null)
            userLists = new ArrayList<>();

        userLists.add(username);
//        for (WebSocketController webSocketController : webSocketControllerSet) {
//            userLists.add(webSocketController.userName);
//        }


        //将所有信息包装好传到客户端(给所有用户)
        Map<String, Object> map1 = new HashMap();
        //  把所有用户列表
        map1.put("onlineUsers", userLists);
        //messageType 1 代表上线 2 代表下线 3 代表私聊消息 4 代表群聊消息
        map1.put("messageType", 1);
        //  返回用户名
        map1.put("username", username);
        //  返回在线人数
        map1.put("number", this.userNumber);
        //  返回Controller
        map1.put("controller", 2);
        //发送给所有用户谁上线了，并让他们更新自己的用户菜单
        sendMessageAll(JSON.toJSONString(map1),this.userName);
        log.info("【websocket消息】有新的连接, 总数:{}", this.userNumber);

//        // 更新在线人数(给所有人)
//        Map<String, Object> map2 = new HashMap();
//        //messageType 1 代表上线 2 代表下线 3 代表私聊消息 4 代表群聊消息
//        map2.put("messageType", 4);
//        //把所有用户放入map2
//        map2.put("onlineUsers", userLists);
//        //返回在线人数
//        map2.put("number", this.userNumber);
//        //返回Controller
//        map2.put("controller", 2);
//        // 消息发送指定人（所有的在线用户信息）
//        sendMessageAll(JSON.toJSONString(map2),this.userName);
    }

    //前端关闭时一个websocket时
    @OnClose
    public void onClose() throws IOException {
        //从集合中移除当前对象
        webSocketControllerSet.remove(this);
        userLists.remove(this.userName);
        //在线用户数减少
        userNumber--;
        Map<String, Object> map1 = new HashMap();
        //messageType 1 代表上线 2 代表下线 3 代表私聊消息 4 代表群聊消息
        map1.put("messageType", 2);
        //所有在线用户
//        map1.put("onlineUsers", this.webSocketControllerSet);
        map1.put("onlineUsers", userLists);
        //下线用户的用户名
        map1.put("username", this.userName);
        //返回在线人数
        map1.put("number", userNumber);
        //返回Controller
        map1.put("controller", 2);
        //发送信息，所有人，通知谁下线了
        sendMessageAll(JSON.toJSONString(map1),this.userName);

        log.info("【websocket消息】连接断开, 总数:{}", webSocketControllerSet.size());
    }

    //前端向后端发送二进制消息（语音）
    @OnMessage
    public void onMessage(byte[] message) throws IOException, EncodeException {
        log.info("【websocket消息】收到客户端发来的二进制消息:{}", message);
//        // 把二进制音频消息数组存入缓冲区(message = 用户名+语音信息，其中用户名占用20byte ）
//        ByteBuffer byteBuffer = ByteBuffer.allocate(message.length-20);
//        byteBuffer.put(message,20,message.length-20);
//        // 前20个字节作为标识储存，方便判断是谁的语音
//        // 提取前20字节内的user信息
//        String toUser = getUsernameByByteArr(Arrays.copyOfRange(message,0,20));
//
//        // 传输字节缓冲区
//        // TODO 根据id判断语音发送对象
//        if (toUser.isEmpty())// 如果是空的,代表群发，则除了自己其他人能听到语音消息
//            sendMessageAll(byteBuffer,this.userName);// 群发
//        sendMessageTo(byteBuffer,toUser);// 对方私聊

        // 把二进制音频消息数组存入缓冲区(message = 用户名+语音信息，其中用户名占用20byte ）
        // 前20个字节作为标识储存，方便判断是谁的语音
        // 提取前20字节内的user信息
        String toUser = getUsernameByByteArr(Arrays.copyOfRange(message,0,20));
        Map<String, Object> map = new HashMap();
        map.put("audioFromUser",this.userName);
        // 传输字节缓冲区
        // TODO 根据id判断语音发送对象
        if (toUser==null) {// 如果是空的,代表群发
            map.put("audioToUser","");
            sendMessageAll(JSON.toJSONString(map),this.userName);// 向前端传输发送方的昵称
            sendMessageAll(ByteBuffer.wrap(message), this.userName);// 群发语音数据
            return;
        }
        map.put("audioToUser",toUser);
        sendMessageTo(JSON.toJSONString(map),toUser);// 向前端发送发送方的昵称
        sendMessageTo(ByteBuffer.wrap(message,20,message.length-20),toUser);// 对方私聊的语音数据
    }



    //前端向后端发送消息
    @OnMessage
    public void onMessage(String message) throws IOException {
        log.info("【websocket消息】收到客户端发来的消息:{}", message);
        //将前端传来的数据进行转型
        JSONObject jsonObject=null;
        try{
            jsonObject = JSON.parseObject(message);
        }catch (Exception e){
//            e.printStackTrace();
            // 具体格式：{"message":"你好呀服务端","username":"orange","type":"群发","toUserName":"写入你要发给的人哦~，默认为空"}
            this.session.getBasicRemote().sendText("消息格式错误，请输入json格式哦~具体格式为：" +
                    "{\"message\":\"你好呀服务端\",\"username\":\"orange\",\"type\":\"群发\",\"toUserName\":\"写入你要发给的人哦~，默认为空\"}");
            return;
        }

        //获取所有数据
        String textMessage = jsonObject.getString("message");
        String username = jsonObject.getString("username");
        Integer type = jsonObject.getInteger("type");
        String toUserName = jsonObject.getString("toUserName");
        Integer controller = jsonObject.getInteger("controller");// 判断Controller逻辑的
        //群发
        if(type == 1){
            Map<String, Object> map3 = new HashMap();
            //messageType 1 代表上线 2 代表下线 3 代表私聊消息 4 代表群聊消息
            map3.put("messageType", 4);
            //所有在线用户
//            map3.put("onlineUsers", this.webSocketControllerSet);
            map3.put("onlineUsers", userLists);
            //发送消息的用户名
            map3.put("username", username);
            //返回在线人数
            map3.put("number", userNumber);
            //发送的消息
            map3.put("textMessage", textMessage);
            //发送的处理控制器
            map3.put("controller", controller);
            //发送信息，所有人，通知谁下线了
            sendMessageAll(JSON.toJSONString(map3),this.userName);
        }
        //私发
        else if (type == 2){
            //发送给对应的私聊用户
            Map<String, Object> map3 = new HashMap();
            //messageType 1 代表上线 2 代表下线 3 代表私聊消息 4 代表群聊消息
            map3.put("messageType", 3);
            //所有在线用户
//            map3.put("onlineUsers", this.webSocketControllerSet);
            map3.put("onlineUsers", userLists);
            //发送消息的用户名
            map3.put("username", username);
            //返回在线人数
            map3.put("number", userNumber);
            //发送的消息
            map3.put("textMessage", textMessage);
            //发送的处理控制器
            map3.put("controller", controller);
            //发送信息，所有人，通知谁下线了
            sendMessageTo(JSON.toJSONString(map3),toUserName);
        }
//        // 在线名单
//        else if(type == 3){
//            Map<String, Object> map4 = new HashMap();
//            map4.put("messageType", 3);
//            //所有在线用户
////            map4.put("onlineUsers", this.webSocketControllerSet);
//            map4.put("onlineUsers", userLists);
//            //发送消息的用户名
//            map4.put("username", username);
//            //返回在线人数
//            map4.put("number", userNumber);
//            //发送的消息
//            map4.put("textMessage", textMessage);
//            //发送的处理控制器
//            map4.put("controller", controller);
//            //发送给自己
//            this.session.getBasicRemote().sendText(JSON.toJSONString(map4));
//        }
    }

    /**
     *  消息发送所有人
     */
    public void sendMessageAll(String message, String FromUserName) throws IOException {
        for (WebSocketController webSocketController : webSocketControllerSet) {
            //消息发送所有人（同步）getAsyncRemote
            webSocketController.session.getBasicRemote().sendText(message);
        }
    }

    /**
     *  消息发送指定人
     */
    public void sendMessageTo(String message, String toUserName) throws IOException {
        //遍历所有用户
        for (WebSocketController webSocketController : webSocketControllerSet) {
            if (webSocketController.userName.equals(toUserName)) {
                //消息发送指定人
                webSocketController.session.getBasicRemote().sendText(message);
                log.info("【发送消息】:", this.userName+"向"+toUserName+"发送消息："+message);
                break;
            }
        }
    }

    // 群发语音消息
    public void sendMessageAll(ByteBuffer message, String FromUserName) throws IOException {
        for (WebSocketController webSocketController : webSocketControllerSet) {
            if (webSocketController.userName.equals(FromUserName))
                continue;// 除了自己
            //消息发送所有人（同步）getAsyncRemote
            webSocketController.session.getBasicRemote().sendBinary(message);
        }
    }

    // 私发发送音频消息
    public void sendMessageTo(ByteBuffer message, String toUserName) throws IOException {
        //遍历所有用户
        for (WebSocketController webSocketController : webSocketControllerSet) {
            if (webSocketController.userName.equals(toUserName)) {
                //消息发送指定人
                webSocketController.session.getBasicRemote().sendBinary(message);
                log.info("【发送消息】:", this.userName+"向"+toUserName+"发送消息："+message);
                break;
            }
        }
    }

    // 通过提取字节流前20个字节提取用户昵称字符串信息
    private String getUsernameByByteArr(byte[] arr) {
        try {
            String user = new String(arr,"gbk");
            String[] split = user.split("\0+");// 以多个0为分割，提取用户昵称信息 因为可能是 0000000orange 这样子
            if (split.length==0)
                return null;
            return split[0];
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

}
