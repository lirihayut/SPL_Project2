package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

public class CrashedBroadcast implements Broadcast {
    private final String message;
    private final String id;

    public CrashedBroadcast(String id, String message) {
        this.id = id;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    public String getId() {
        return id;
    }

}
