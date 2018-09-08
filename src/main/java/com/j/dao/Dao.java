package com.j.dao;

import com.j.domain.UserRecord;

import java.util.List;

public interface Dao<T> {
    List<T> getAll();

    void add(T t);

    UserRecord getUserById(String userId);
}
