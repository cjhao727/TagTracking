package com.j.dao;

import com.j.domain.UserTagData;

import java.util.List;

public interface Dao<T> {
    List<T> getAll();

    void add(T t);

    UserTagData getUserById(String userId);
}
