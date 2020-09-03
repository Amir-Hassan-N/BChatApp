package com.example.bchatapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {

    @Query("SELECT * FROM user")
    List<User> getAll();

    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
    List<User> loadAllByIds(int[] userIds);

    @Query("SELECT * FROM user WHERE Message LIKE :msg_box LIMIT 1")
    User findByName(String msg_box);

    @Insert
    void insertAll(User... users);

    @Delete
    void delete(User user);
}
