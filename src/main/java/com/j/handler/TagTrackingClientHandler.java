package com.j.handler;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.j.dao.Dao;
import com.j.dao.UserDaoImpl;
import com.j.domain.UserOperationRecord;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TagTrackingClientHandler extends Thread {
    private Socket tagTrackingClientSocket;
    private Dao<UserRecord> userDao = new UserDaoImpl();
    private Gson gson;
    private PrintWriter out;

    public TagTrackingClientHandler(Socket socket) {
        this.tagTrackingClientSocket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(tagTrackingClientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(tagTrackingClientSocket.getInputStream()));
            String inputLine;
            gson = new Gson();
            while ((inputLine = in.readLine()) != null) {
                // get request
                TagRequest tagRequest = gson.fromJson(inputLine, TagRequest.class);
                // extract info from request
                String currentUserId = tagRequest.getUser();
                String currentRequestTime = tagRequest.getTimestamp();
                //1) If a tag appears multiple times in either ​add​ or ​remove​, it's equivalent to appearing once.
                List<String> currentAddOps = tagRequest.getAdd();
                currentAddOps = currentAddOps.stream().distinct().collect(Collectors.toList());
                List<String> currentRemoveOps = tagRequest.getRemove();
                currentRemoveOps = currentRemoveOps.stream().distinct().collect(Collectors.toList());
                //2) If a tag appears in both ​add​ and ​remove​ it should be treated as if it only appeared in ​remove​.
                currentAddOps.removeAll(currentRemoveOps);

                UserOperationRecord currentUserOperationRecord = new UserOperationRecord(currentRequestTime, currentAddOps, currentRemoveOps);
                List<UserOperationRecord> currentUserOpRecords = new ArrayList<>();
                currentUserOpRecords.add(currentUserOperationRecord);
                UserRecord currentUserRecord = new UserRecord(currentUserId, currentUserOpRecords, new ArrayList<>());

                //check if current user record exits, if not build response directly.
                List<UserRecord> existingUserRecords = userDao.getAll();
                Optional<UserRecord> targetUserRecord = existingUserRecords.stream()
                        .filter(userRecord -> userRecord.getUserId().equals(currentUserRecord.getUserId()))
                        .findFirst();
                if (existingUserRecords.isEmpty() || !targetUserRecord.isPresent()) {
                    userDao.add(currentUserRecord);
                    currentUserRecord.getTagCollection().addAll(currentAddOps);
                    currentUserRecord.getTagCollection().removeAll(currentRemoveOps);

                    TagResponse tagResponse = new TagResponse(
                            currentUserId,
                            currentUserRecord
                                    .getTagCollection()
                                    .stream()
                                    .distinct()
                                    .collect(Collectors.toList()));
                    out.println(gson.toJson(tagResponse));
                } else {
                    // tricky part to build json response, now just focus on in order requests, todo: out of order
                    targetUserRecord.get().getUserOperationRecords().add(new UserOperationRecord(currentRequestTime, currentAddOps, currentRemoveOps));

                    targetUserRecord.get().getUserOperationRecords().sort(Comparator.comparing(UserOperationRecord::getTimestamp));

                    targetUserRecord.get().getUserOperationRecords().forEach(record -> {
                        targetUserRecord.get().getTagCollection().addAll(record.getAddOperation());
                        targetUserRecord.get().getTagCollection().removeAll(record.getRemoveOperation());
                    });

                    TagResponse tagResponse = new TagResponse(
                            currentUserId,
                            targetUserRecord.get()
                                    .getTagCollection()
                                    .stream()
                                    .distinct()
                                    .collect(Collectors.toList()));
                    out.println(gson.toJson(tagResponse));

                }
            }

            in.close();
            out.close();
            tagTrackingClientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            //If any issues arise, such as a request cannot be parsed, the server should instead write an error response JSON body, compressed to one line
            String errorJsonResponse = buildErrorJsonResponse(e);
            out.println(errorJsonResponse);
        }
    }

    private String buildErrorJsonResponse(JsonSyntaxException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return gson.toJson(errorResponse);
    }
}
