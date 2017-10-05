package com.example.xiangyunlin.mysearchviewdemo;

/**
 * Created by XiangYunLin on 2017/10/5.
 */

public class Contacts {
    private String mName;
    private String mPhoneNumber;

    public Contacts(String name, String phoneNumber) {
        this.mName = name;
        this.mPhoneNumber = phoneNumber;
    }

    public String getmName() { return this.mName; }

    public String getmPhoneNumber() { return this.mPhoneNumber; }

    public void setmName(String name) {
        this.mName = name;
    }

    public void setmPhoneNumber(String phoneNumber) {
        this.mPhoneNumber = phoneNumber;
    }

}
