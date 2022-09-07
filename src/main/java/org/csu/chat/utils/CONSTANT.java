package org.csu.chat.utils;

import lombok.Getter;

public class CONSTANT {
    @Getter
    public enum ControllerChoose{
        AUDIO_CONTROLLER(1,"音频控制器"),
        CHAT_CONTROLLER(2,"聊天室控制器");

        private final int code;
        private final String description;
        ControllerChoose(int code, String description){
            this.code = code;
            this.description = description;
        }
    }

    /**
     * 客户端接受的消息类型
     */
    @Getter
    public enum MessageType{
        ONLINE_MESSAGE(1,"上线消息"),
        OFFLINE_MESSAGE(2,"下线消息"),
        PRIVATE_MESSAGE(3,"私信消息"),
        PUBLIC_MESSAGE(4,"群发消息");

        private final int code;
        private final String description;
        MessageType(int code, String description){
            this.code = code;
            this.description = description;
        }
    }

    /**
     * 服务端发送的消息类型
     */
    @Getter
    public enum MessageToServerType{
        PUBLIC_MESSAGE(1,"群发消息"),
        PRIVATE_MESSAGE(2,"私聊消息"),
        OFFLINE_USERLIST(3,"获取在线名单");

        private final int code;
        private final String description;
        MessageToServerType(int code, String description){
            this.code = code;
            this.description = description;
        }
    }
}
