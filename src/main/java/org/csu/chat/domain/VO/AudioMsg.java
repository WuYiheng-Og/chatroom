package org.csu.chat.domain.VO;

import lombok.Data;

@Data
public class AudioMsg {
    private Integer id;// 语音条id
    private byte[] audio; // 音频记录
    private String senderName; // 音频发送者
}
