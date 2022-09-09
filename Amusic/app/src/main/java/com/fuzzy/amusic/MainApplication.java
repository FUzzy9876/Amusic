package com.fuzzy.amusic;

import android.app.Application;

import androidx.room.Room;

import com.fuzzy.amusic.database.SongDao;
import com.fuzzy.amusic.database.SongDatabase;
import com.fuzzy.amusic.utils.CsvFileReader;
import com.fuzzy.amusic.utils.MusicService;

public class MainApplication extends Application {
    private static MainApplication instance;
    private static MusicService musicService;
    private static CsvFileReader csvFileReader;
    private static SongDao songDao;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        musicService = new MusicService();
        csvFileReader = new CsvFileReader();
        // songDao = Room.databaseBuilder(getInstance(), SongDatabase.class, "song-database").build().songDao();
    }

    @Override
    public void onTerminate() {
        musicService.onDestroy();
        super.onTerminate();
    }

    public static MainApplication getInstance() {
        if (instance == null) {
            return new MainApplication();
        }
        else {
            return instance;
        }
    }

    public static MusicService getMusicService() {
        if (instance == null) {
            return new MusicService();
        }
        else {
            return musicService;
        }
    }

    public static CsvFileReader getCsvFileReader() {
        if (instance == null) {
            return new CsvFileReader();
        }
        else {
            return csvFileReader;
        }
    }

    public static SongDao getSongDao() {
        if (songDao == null) {
            return Room.databaseBuilder(getInstance(), SongDatabase.class, "song-database").build().songDao();
        }
        else {
            return songDao;
        }
    }
}
