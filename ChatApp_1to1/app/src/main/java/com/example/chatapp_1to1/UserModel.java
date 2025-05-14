package com.example.chatapp_1to1;

public class UserModel {
    public String email;
    public String password;
    public String username;

    public UserModel() {}  // Firebase 용 기본 생성자

    public UserModel(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }
}
