package com.mq.myqq.entity;

public class User {
    private String nickname;

    private String password;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
    }
}
