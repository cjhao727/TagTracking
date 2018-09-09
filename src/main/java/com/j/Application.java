package com.j;

import com.j.server.TagTrackingServer;

public class Application {
    private static final int port = 5555;
    public static void main(String[] args) {

        TagTrackingServer tagTrackingServer = new TagTrackingServer();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nPrepare to exit");
            tagTrackingServer.stop();
            System.out.println("Tag tracking server closed.");
        }));

        System.out.println("Tag tracking server started @ localhost:" + port);
        tagTrackingServer.start(port);
    }
}
