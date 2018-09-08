package com.j.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.j.dao.Dao;
import com.j.dao.UserDaoImpl;
import com.j.domain.UserRecord;
import com.j.domain.request.TagRequest;
import com.j.domain.response.ErrorResponse;
import com.j.domain.response.TagResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TagTrackingClientHandler extends Thread {
    private Socket tagTrackingClientSocket;
    private Dao<UserRecord> userDao = new UserDaoImpl();
    private Gson gson;
    private PrintWriter out;
    private BufferedReader in;

    public TagTrackingClientHandler(Socket socket) {
        this.tagTrackingClientSocket = socket;
    }

    @Override
    public void run() {
        try /*(
                PrintWriter out = new PrintWriter(tagTrackingClientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(tagTrackingClientSocket.getInputStream()))
        ) */{
            out = new PrintWriter(tagTrackingClientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(tagTrackingClientSocket.getInputStream()));
            String inputLine;
            gson = new Gson();
            while ((inputLine = in.readLine()) != null) {
                if (".".equals(inputLine)) {
                    out.println("bye");
                    break;
                }

                // get request
                TagRequest tagRequest = gson.fromJson(inputLine, TagRequest.class);

                // get userId
                String userId = tagRequest.getUser();
                // check if user exists, if not add
                ifUserRecordAbsentThenAdd(userId);

                // process operations of request
                //1) If a tag appears multiple times in either ​add​ or ​remove​, it's equivalent to appearing once.
                List<String> tagsNeedToAdd = tagRequest.getAdd().stream().distinct().collect(Collectors.toList());
                List<String> tagsNeedToRemove = tagRequest.getRemove().stream().distinct().collect(Collectors.toList());

                //2) If a tag appears in both ​add​ and ​remove​ it should be treated as if it only appeared in ​remove​.
                tagsNeedToAdd.removeAll(tagsNeedToRemove);

                //execute operations to update the record
                UserRecord userRecord = userDao.getUserById(userId);

                tagsNeedToAdd = tagsNeedToAdd.stream().filter(tag -> !userRecord.getTagCollection().contains(tag)).collect(Collectors.toList());
                userRecord.getTagCollection().addAll(tagsNeedToAdd);

                //It's not an error to attempt to remove from a user a tag that it does not currently have.
                userRecord.getTagCollection().removeAll(tagsNeedToRemove);

                //build json response
                buildJsonResponse(out, userId, userRecord);

                // sweep
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            //If any issues arise, such as a request cannot be parsed, the server should instead write an error response JSON body, compressed to one line
            String errorJsonResponse = buildErrorJsonResponse(e);
            out.println(errorJsonResponse);
            out.close();
        }
    }

    private void ifUserRecordAbsentThenAdd(String userId) {
        List<UserRecord> userRecords = userDao.getAll();
        Optional<UserRecord> existingUserRecord = userRecords.stream().filter(userRecord -> userRecord.getUserId().equals(userId)).findFirst();
        if (userRecords.isEmpty() || !existingUserRecord.isPresent()) {
            UserRecord userRecord = new UserRecord(userId, new ArrayList<>());
            userDao.add(userRecord);
        }
    }

    private void buildJsonResponse(PrintWriter out, String userId, UserRecord userRecord) {
        TagResponse tagResponse = new TagResponse(userId, userRecord.getTagCollection());
        String tagResponseJson = gson.toJson(tagResponse);
        out.println(tagResponseJson);
    }

    private String buildErrorJsonResponse(JsonSyntaxException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return gson.toJson(errorResponse);
    }
}
