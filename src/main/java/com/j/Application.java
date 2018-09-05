package com.j;

import com.j.server.TagTrackingServer;

public class Application {
    public static void main(String[] args) {
        TagTrackingServer tagTrackingServer = new TagTrackingServer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nPrepare to exit");
            tagTrackingServer.stop();
            System.out.println("Tag tracking server closed.");
        }));
        tagTrackingServer.start(5555);
    }
}
