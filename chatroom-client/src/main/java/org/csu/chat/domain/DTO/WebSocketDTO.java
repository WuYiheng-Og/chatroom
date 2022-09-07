package org.csu.chat.domain.DTO;

import lombok.Data;

@Data
public class WebSocketDTO {
    private String message;// 消息内容
    private String username;// 用户
    private Integer type;// 1 群发 2 私聊 3 获取在线用户信息（弃用）
    private String toUserName;
    private Integer controller;  //用来判断是那个Controller，来处理具体逻辑 1.ChatRoom 2.Audio
    // 吐槽，我真的想念信号与槽，只怪websocket获取信息的处理必须在websocket的类内方法覆盖写，我又不想写类内方法，只能疯狂写静态类，真的栓Q[主要还是菜
}
