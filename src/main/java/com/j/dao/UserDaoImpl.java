package com.j.dao;

import com.j.domain.UserTagData;

import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements Dao<UserTagData> {
    //mimic DB
    private List<UserTagData> userTagData = new ArrayList<>();

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
