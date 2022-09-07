package org.csu.chat.domain.VO;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Message {
    private String senderName;
    private String message;
    private Timestamp timestamp;

    public Message(String senderName, String message, Timestamp timestamp) {
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }
}
