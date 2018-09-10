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
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
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
                TagRequest tagRequest = gson.fromJson(inputLine, TagRequest.class);

                String currentUserId = tagRequest.getUser();
                String currentRequestTime = tagRequest.getTimestamp();

                //1) If a tag appears multiple times in either ​add​ or ​remove​, it's equivalent to appearing once.
                List<String> currentAddOps = tagRequest.getAdd();
                currentAddOps = currentAddOps.stream().distinct().collect(Collectors.toList());

                List<String> currentRemoveOps = tagRequest.getRemove();
                currentRemoveOps = currentRemoveOps.stream().distinct().collect(Collectors.toList());

                //2) If a tag appears in both ​add​ and ​remove​ it should be treated as if it only appeared in ​remove​.
                currentAddOps.removeAll(currentRemoveOps);

                UserRecord currentUserRecord = buildCurrentUserRecord(currentUserId, currentRequestTime, currentAddOps, currentRemoveOps);

                List<UserRecord> existingUserRecords = userDao.getAll();
                Optional<UserRecord> targetUserRecord = existingUserRecords.stream()
                        .filter(userRecord -> userRecord.getUserId().equals(currentUserRecord.getUserId()))
                        .findFirst();

                if (existingUserRecords.isEmpty() || !targetUserRecord.isPresent()) {
                    //ifAbsentThenAdd
                    userDao.add(currentUserRecord);
                    TagResponse tagResponse = buildTagResponse(currentUserId, currentAddOps, currentRemoveOps, currentUserRecord);
                    out.println(gson.toJson(tagResponse));
                } else {
                    targetUserRecord.get().getUserOperationRecords().add(new UserOperationRecord(currentRequestTime, currentAddOps, currentRemoveOps));
                    targetUserRecord.get().getUserOperationRecords().sort(Comparator.comparing(UserOperationRecord::getTimestamp));
                    TagResponse tagResponse = buildTagResponse(currentUserId, targetUserRecord);
                    out.println(gson.toJson(tagResponse));
                }
            }

            in.close();
            out.close();
            tagTrackingClientSocket.close();

        } catch (IOException e) {
            String errorJsonResponse = buildErrorJsonResponse(e);
            out.println(gson.toJson(errorJsonResponse));
        } catch (JsonSyntaxException e) {
            String errorJsonResponse = buildErrorJsonResponse(e);
            out.println(errorJsonResponse);
        }
    }

    @NotNull
    private TagResponse buildTagResponse(String currentUserId, Optional<UserRecord> targetUserRecord) {
        targetUserRecord.get().getUserOperationRecords().forEach(record -> {
            targetUserRecord.get().getTagCollection().addAll(record.getAddOperation());
            targetUserRecord.get().getTagCollection().removeAll(record.getRemoveOperation());
        });

        return new TagResponse(currentUserId,
                targetUserRecord.get()
                        .getTagCollection()
                        .stream()
                        .distinct()
                        .collect(Collectors.toList()));
    }

    @NotNull
    private TagResponse buildTagResponse(String currentUserId, List<String> currentAddOps, List<String> currentRemoveOps, UserRecord currentUserRecord) {
        currentUserRecord.getTagCollection().addAll(currentAddOps);
        currentUserRecord.getTagCollection().removeAll(currentRemoveOps);

        return new TagResponse(currentUserId,
                currentUserRecord
                .getTagCollection()
                .stream()
                .distinct()
                .collect(Collectors.toList()));
    }

    @NotNull
    private UserRecord buildCurrentUserRecord(String currentUserId, String currentRequestTime, List<String> currentAddOps, List<String> currentRemoveOps) {
        UserOperationRecord currentUserOperationRecord = new UserOperationRecord(currentRequestTime, currentAddOps, currentRemoveOps);
        List<UserOperationRecord> currentUserOpRecords = new ArrayList<>();
        currentUserOpRecords.add(currentUserOperationRecord);
        return new UserRecord(currentUserId, currentUserOpRecords, new ArrayList<>());
    }

    private String buildErrorJsonResponse(JsonSyntaxException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return gson.toJson(errorResponse);
    }

    private String buildErrorJsonResponse(IOException e) {
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return gson.toJson(errorResponse);
    }
}
