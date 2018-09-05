package com.j.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TagTrackingClientHandler extends Thread {
    private Socket tagTrackingClientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public TagTrackingClientHandler(Socket socket) {
        this.tagTrackingClientSocket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(tagTrackingClientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(tagTrackingClientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (".".equals(inputLine)) {
                    out.println("bye");
                    break;
                }
                out.println(inputLine);
            }


            in.close();
            out.close();
            tagTrackingClientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
