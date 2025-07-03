package com.mycompany.lovegarden;

public class UserModel {
    public String email;
    public String password;
    public String username;
    public String code;

    public UserModel() {}  // Firebase 용 기본 생성자

    public UserModel(String email, String password, String username, String code) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.code = code;
    }
}
