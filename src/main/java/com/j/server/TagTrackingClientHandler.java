package com.j.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.j.dao.Dao;
import com.j.dao.UserDaoImpl;
import com.j.domain.UserTagData;
import com.j.domain.request.TagRequest;
import com.j.domain.response.ErrorResponse;
import com.j.domain.response.TagResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TagTrackingClientHandler extends Thread {
    private Socket tagTrackingClientSocket;
    private Dao<UserTagData> userDao = new UserDaoImpl();

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
            Gson gson = new Gson();
            while ((inputLine = in.readLine()) != null) {

                if (".".equals(inputLine)) {
                    out.println("bye");
                    break;
                }

                //todo: concurrentHashMap <user, set<tag>>
                TagRequest tagRequest = gson.fromJson(inputLine, TagRequest.class);
                Set<String> tagSetOfUser = getTagSetOfUser(tagRequest);
                updateTagSetOfUser(tagRequest, tagSetOfUser);
                buildJsonOutput(out, gson, tagRequest, tagSetOfUser);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            String jsonErrorMessage = new Gson().toJson(errorResponse);
            System.out.println(jsonErrorMessage);
        }
    }

    private Set<String> getTagSetOfUser(TagRequest tagRequest) {
        ifUserAbsentThenAdd(tagRequest);
        return userDao.getUserById(tagRequest.getUser()).getTagSet();
    }

    private void ifUserAbsentThenAdd(TagRequest tagRequest) {
        String userId = tagRequest.getUser();

        List<UserTagData> userTagDataList = userDao.getAll();
        Optional<UserTagData> existingUserTagData = userTagDataList
                .stream()
                .filter(userTagData -> userTagData.getUserId().equals(userId))
                .findFirst();

        if (userTagDataList.size() == 0 || !existingUserTagData.isPresent()) {
            userDao.add(new UserTagData(userId, tagRequest.getAdd()));
        }
    }

    private void updateTagSetOfUser(TagRequest tagRequest, Set<String> tagSetOfUser) {
        tagSetOfUser.addAll(tagRequest.getAdd());
        tagRequest.getRemove().forEach(tagSetOfUser::remove);
    }

    private void buildJsonOutput(PrintWriter out, Gson gson, TagRequest tagRequest, Set<String> tagSetOfUser) {
        TagResponse tagResponse = new TagResponse(tagRequest.getUser(), tagSetOfUser);
        String tagResponseJson = gson.toJson(tagResponse);
        out.println(tagResponseJson);
    }
}
