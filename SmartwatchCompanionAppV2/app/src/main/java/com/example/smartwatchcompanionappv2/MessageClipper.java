package com.example.smartwatchcompanionappv2;


//basically just splits up a string and gives one part of it at a time
//good for use in the BLEGatt class
public class MessageClipper {
    private String message = "";
    private int position = 0;
    private int clipSize = 16;


    public MessageClipper(String message) {
        this.message = message;
        clipSize = 16;
    }

    public MessageClipper(String message, int clipSize) {
        this.clipSize = clipSize;
        this.message = message;
    }

    public boolean messageComplete(){
        return position == message.length();
    }

    public String getFullMessage(){
        return message;
    }


    public String getNextMessage() {
        String ret = "";
        if (position + clipSize < message.length()) {
            ret = message.substring(position, position + clipSize);
            position += clipSize;
        } else if (position + clipSize > message.length()) {
            ret = message.substring(position);
            position = message.length();
        }
        return ret;
    }
}
