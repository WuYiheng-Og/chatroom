package org.csu.chat.utils;

import javafx.scene.layout.AnchorPane;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 存储wss交互信息（类似session）
 */
@Data
public class WSStorage {
    private static WSStorage instance;

    public WSStorage() {
        audioHistoryList = new ArrayList<>();
        audioMap = new HashMap<>();
    }

    public static synchronized WSStorage getInstance(){
        if (instance == null)
            instance = new WSStorage();
        return instance;
    }
    private String loginUser;
    private String fromUsername; //接收消息角色，如果是1，则为上线用户；2，下线用户；3，接收在线名单的用户（即本人）；4，私聊，发消息的人
    private String message; // 接收到的消息
    private Integer onlineNumber; // 在线人数
    private Integer messageType; // 1 代表上线 2代表下线 3 代表在线名单 4 代表普通消息即私发
    private List<String> onlineUsers;

    private byte[] audioByteArr;// 音频数据
    private String audioSender;// 发送语音的用户
    private String audioReceiver;// 接收语音的用户，若为空则为群发
    private List<String> audioHistoryList;// 语音条历史记录 如第一条是orange发的，则为 1：orange
    private HashMap<Integer,byte[]> audioMap;// 根据语音条次序来匹配

    private AnchorPane curPlayPane; // 当前正在播放的pane，用于控制gif的暂停开始
    private Boolean isClick=false; // 状态控制，若语音消息已经被点击，则为true，此时无法点击语音消息；否则为false，可以点击语音消息播放（这样子防止用户重复点击）
}
