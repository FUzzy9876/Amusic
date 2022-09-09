package com.fuzzy.amusic.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SongDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<Song>

    @Query("SELECT * FROM user WHERE id LIKE :songId ")
    fun findById(songId: String): Song

    @Insert
    fun insertAll(vararg users: Song)

    @Delete
    fun delete(user: Song)
}