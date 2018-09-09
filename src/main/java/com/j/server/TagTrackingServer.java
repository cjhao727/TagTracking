package com.j.server;

import com.j.handler.TagTrackingClientHandler;

import java.io.IOException;
import java.net.ServerSocket;

public class TagTrackingServer {

    private ServerSocket serverSocket;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            while (true) new TagTrackingClientHandler(serverSocket.accept()).start();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
