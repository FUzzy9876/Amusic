package com.fuzzy.amusic.utils

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.PlayActivity
import com.fuzzy.amusic.database.Song
import kotlinx.coroutines.*

class MusicService : Service() {
    private val player: MediaPlayer = MediaPlayer()

    private var playActivity: PlayActivity? = null

    val playlist: MutableList<Song> = mutableListOf()
    private val playlistId: MutableSet<String> = mutableSetOf()
    private val cachedId: MutableSet<String> = mutableSetOf()
    val cachingList: MutableList<Song> = mutableListOf()
    var autoCachePlaylist: Boolean = true

    val onCacheStartCallbacks: MutableList<() -> Unit> = mutableListOf()
    val onCacheEndCallbacks: MutableList<() -> Unit> = mutableListOf()

    val mode: MutableMap<String, String> = mutableMapOf(("random" to "off"), ("repeat" to "on"))

    var cachingNum: Int = 0
        set(value) {
            val origin: Int = field
            field = value
            if (origin == 0 && field > 0) {
                for (func in onCacheStartCallbacks) {
                    func()
                }
            }
            if (origin > 0 && field == 0) {
                for (func in onCacheEndCallbacks) {
                    func()
                }
            }
        }
    var currentIndex: Int = -1
    val currentSong: Song?
        get() {
            return if (0 <= currentIndex && currentIndex < playlist.size) {
                playlist[currentIndex]
            } else {
                null
            }
        }
    var isLoad: Boolean = false
        set(value) {
            field = value
            if (value) {
                playActivity?.onSongLoad(currentIndex, currentSong)
            }
            else {
                playActivity?.onSongUnload()
            }
        }
    val isPlaying: Boolean
        get() {
            return player.isPlaying
        }
    val currentPosition: Int
        get() {
            return player.currentPosition
        }
    val duration: Int
        get() {
            return player.duration
        }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun cache(song: Song) {
        if (song.isCached()) {
            Log.i("MusicService / cache", "$song exists in cache file")
            cachedId.add(song.id)
        }
        else {
            CoroutineScope(Dispatchers.IO).launch {
                cachingList.add(song)
                Log.i("MusicService / cache", "current cache list size is ${cachingList.size}")
                val downloadResult: Boolean = Network().downloadSong(song)
                if (downloadResult) {
                    Log.i("MusicService / cache", "$song cached successfully")
                    cachingList.remove(song)
                    cachedId.add(song.id)
                }
            }
        }
    }

    fun addToPlaylist(song: Song, position: Int = -1, save: Boolean = true) {
        if (song.id in playlistId) {
            Log.i("MusicService / playlist", "$song already in playlist")
            Toast.makeText(getInstance(), "重复添加", Toast.LENGTH_SHORT).show()
        }
        else {
            if (position == -1) {
                playlist.add(song)
            }
            else {
                playlist.add(position, song)
            }
            playlistId.add(song.id)
            if (autoCachePlaylist) {
                cache(song)
            }
        }

        playActivity?.onPlaylistChange()

        if (save) {
            CoroutineScope(Dispatchers.IO).launch {
                ConfigParser().savePlaylist(playlist)
            }
        }
    }

    fun delFromPlaylist(song: Song, save: Boolean = true) {
        if (song.id in playlistId) {
            for (i in playlist.indices) {
                if (song.id == playlist[i].id) {
                    playlist.removeAt(i)
                    if (i < currentIndex) {
                        currentIndex -= 1
                    }
                    else if (i == currentIndex) {
                        load()
                    }
                    break
                }
            }
            playlistId.remove(song.id)

            playActivity?.onPlaylistChange()

            if (save) {
                CoroutineScope(Dispatchers.IO).launch {
                    ConfigParser().savePlaylist(playlist)
                }
            }
        }
        else {
            Log.w("MusicService / del", "$song is not in playlist")
        }
    }

    fun load(autoPlay: Boolean = true, onComplete: () -> Unit = {}) {
        if (playlist.isEmpty()) {
            Log.w("MusicService / load", "playlist is empty, no songs to load")
            Toast.makeText(getInstance(), "播放列表为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentIndex < 0 || currentIndex >= playlist.size) {
            Log.w("MusicService / load", "playlist size is ${playlist.size}, but current index is $currentIndex, set it to 0")
            currentIndex = 0
        }
        val song: Song = playlist[currentIndex]

        CoroutineScope(Dispatchers.Main).launch {
            Log.i("MusicService / load", "waiting for cache file")
            while (song.id !in cachedId) {
                delay(50)
            }
            Log.i("MusicService / load", "ready to load")

            kotlin.runCatching {
                player.reset()
                player.setDataSource(song.toPath())
                player.prepare()
                player.isLooping = false
                player.setOnCompletionListener { onPlayComplete() }
            }.onSuccess {
                Log.i("MusicService / load", "success")
                isLoad = true
                if (autoPlay) {
                    Log.i("MusicService / load", "auto play")
                    play()
                }
                onComplete()
            }.onFailure {
                Log.e("MusicService / load", "error: ", it)
                onComplete()
            }
        }
    }

    fun play(onComplete: () -> Unit = {}) {
        if (!isLoad) {
            Log.w("MusicService / play", "no song has load, will auto load a song, but always load a song fist")
            load(true)
            return
        }
        Log.i("MusicService", "play")
        player.start()

        playActivity?.onPlayerStart()
    }

    fun pause() {
        Log.i("MusicService", "pause")
        player.pause()

        playActivity?.onPlayerPause()
    }

    fun stop() {
        player.stop()

        playActivity?.onPlayerStop()
    }

    fun seek(msec: Int, onComplete: () -> Unit) {
        player.setOnSeekCompleteListener {
            onComplete()
        }
        player.seekTo(msec)
    }

    fun switchToSong(song: Song, autoPlay: Boolean) {
        if (song.id in playlistId) {
            currentIndex = playlistId.indexOf(song.id)
        }
        else {
            addToPlaylist(song, currentIndex + 1)
            currentIndex += 1
        }
        load(autoPlay)
    }

    fun onPlayComplete() {
        playNext(true)
    }

    fun playPrev(play: Boolean = false) {
        if (mode["random"] == "on") {
            val exclude: Int = currentIndex
            var nextIndex: Int = (0 until playlist.size).random()
            while (nextIndex == exclude) {
                nextIndex = (0 until playlist.size).random()
            }
            currentIndex = nextIndex
        }
        else {
            if (mode["repeat"] != "one") {
                if (currentIndex - 1 > 0) {
                    currentIndex -= 1
                }
                else {
                    if (mode["repeat"] == "on") {
                        currentIndex = playlist.size - 1
                    }
                    else {
                        Toast.makeText(getInstance(), "已经是第一个", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }
        load(play)
    }

    fun playNext(play: Boolean = false) {
        if (mode["random"] == "on") {
            val exclude: Int = currentIndex
            var nextIndex: Int = (0 until playlist.size).random()
            while (nextIndex == exclude) {
                nextIndex = (0 until playlist.size).random()
            }
            currentIndex = nextIndex
        }
        else {
            if (mode["repeat"] != "one") {
                if (currentIndex + 1 < playlist.size) {
                    currentIndex += 1
                }
                else {
                    if (mode["repeat"] == "on") {
                        currentIndex = 0
                    }
                    else {
                        Toast.makeText(getInstance(), "已经是最后一个", Toast.LENGTH_SHORT).show()
                        return
                    }
                }
            }
        }
        load(play)
    }

    fun setPlayActivity(activity: PlayActivity?) {
        Log.i("MusicService", "get current play activity: $activity")
        playActivity = activity
    }
}

