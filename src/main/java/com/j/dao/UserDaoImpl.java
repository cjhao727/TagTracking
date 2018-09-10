package com.j.dao;

import com.j.domain.UserRecord;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserDaoImpl implements Dao<UserRecord> {
    //mimic DB
    private List<UserRecord> userRecords = new CopyOnWriteArrayList<>();

    @Override
    public List<UserRecord> getAll() {
        return userRecords;
    }

    @Override
    public void add(UserRecord userRecord) {
        this.userRecords.add(userRecord);
    }

    @Override
    public UserRecord getUserById(String userId) {
        // assume always have one match
        return userRecords.stream().filter(userRecord -> userRecord.getUserId().equals(userId)).findFirst().get();
    }
}
