package org.csu.chat.domain.VO;

import lombok.Data;

@Data
public class Member {
    private String avatar;
    private String name;
    private Boolean status;

    public Member(String avatar, String name, Boolean status) {
        this.avatar = avatar;
        this.name = name;
        this.status = status;
    }
}
