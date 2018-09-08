package com.j.dao;

import com.j.domain.UserRecord;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserDaoImpl implements Dao<UserRecord> {
    //mimic DB, thread Safe list
    private List<UserRecord> userTagData = new CopyOnWriteArrayList<>();

    @Override
    public List<UserRecord> getAll() {
        return userTagData;
    }

    @Override
    public void add(UserRecord userRecord) {
        this.userTagData.add(userRecord);
    }

    @Override
    public UserRecord getUserById(String userId) {
        // assume always have one match
        return userTagData.stream().filter(userRecord -> userRecord.getUserId().equals(userId)).findFirst().get();
    }
}
