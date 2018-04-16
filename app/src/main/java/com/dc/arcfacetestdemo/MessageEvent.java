package com.dc.arcfacetestdemo;

import java.util.ArrayList;

/**
 * Created by zsn10 on 2017/10/17.
 */

public class MessageEvent {

    private  ArrayList<Contact> mContactList;
    private  String mMsg;

    public MessageEvent(String mMsg,ArrayList<Contact> contactList) {
        this.mMsg = mMsg;
        this.mContactList=contactList;
    }
    public ArrayList<Contact> getmContactList(){
        return mContactList;
    }
    public String getmMsg(){
        return mMsg;
    }
}
