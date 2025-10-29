package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;


public class TerminatedBroadcast implements Broadcast {
    private String message;
    public TerminatedBroadcast(String message){

        this.message = message;
    }
    public String getMessage (){
        return message;
    }
}
