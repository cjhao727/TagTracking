package com.j.dao;

import com.j.domain.UserTagData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class UserDaoImpl implements Dao<UserTagData> {
    //mimic DB, thread Safe list
    private List<UserTagData> userTagData = new CopyOnWriteArrayList<>();

    @Override
    public List<UserTagData> getAll() {
        return userTagData;
    }

    @Override
    public void add(UserTagData userTagData) {
        this.userTagData.add(userTagData);
    }

    @Override
    public UserTagData getUserById(String userId) {
        // assume always have one match
        return userTagData.stream().filter(userTagData -> userTagData.getUserId().equals(userId)).findFirst().get();
    }
}
