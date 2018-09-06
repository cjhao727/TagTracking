package com.j.server;

import com.google.gson.Gson;
import com.j.domain.request.TagRequest;
import com.j.domain.response.TagResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class TagTrackingClientHandler extends Thread {
    private Socket tagTrackingClientSocket;

    public TagTrackingClientHandler(Socket socket) {
        this.tagTrackingClientSocket = socket;
    }

    @Override
    public void run() {
        try (
                PrintWriter out = new PrintWriter(tagTrackingClientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(tagTrackingClientSocket.getInputStream()))
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (".".equals(inputLine)) {
                    out.println("bye");
                    break;
                }
                TagRequest tagRequest = new Gson().fromJson(inputLine, TagRequest.class);
                List<String> tags = tagRequest.getAdd();
                tagRequest.getRemove().forEach(t -> tags.removeIf(tag -> tag.equals(t)));
                TagResponse tagResponse = new TagResponse(tagRequest.getUser(), tags);
                String tagResponseJson = new Gson().toJson(tagResponse);
                out.println(tagResponseJson);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
