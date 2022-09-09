package com.fuzzy.amusic.utils

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.database.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class MusicService : Service() {
    private var musicPlayer: MediaPlayer = MediaPlayer()

    val playlist: MutableList<Song> = mutableListOf()
    val playlistIdSet: MutableSet<String> = mutableSetOf()

    private val cacheChannel: Channel<Song?> = Channel()
    private val cacheCompleteChannel: Channel<String?> = Channel()

    val titleMessageChannel: Channel<String?> = Channel()
    val playlistMessageChannel: Channel<MutableList<Song>?> = Channel()

    var currentSong: Song? = null
    var currentIndex: Int = -1
    var isSongLoad: Boolean = false

    var playMode: MutableMap<String, String> = mutableMapOf("random" to "false", "repeat" to "on")

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        stopCacheSong()
        release()

        titleMessageChannel.close()
        playlistMessageChannel.close()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun release() {
        musicPlayer.release()
    }

    fun setCurrent(index: Int) {
        var title = ""
        if (index == -1) {
            currentIndex = -1
            currentSong = null
        }
        else {
            currentIndex = index
            currentSong = playlist[index]
            title = currentSong!!.name
        }
        CoroutineScope(Dispatchers.Default).launch {
            Log.i("setCurrent", "sending message: $currentIndex, $title")
            titleMessageChannel.send(title)
            playlistMessageChannel.send(playlist)
        }
    }

    fun prepareThenPlay(onCompleteCallBack: () -> Unit = {}) {
        CoroutineScope(Dispatchers.Main).launch {
            if (currentIndex == -1) { setCurrent(0) }
            currentSong = playlist[currentIndex]

            withContext(Dispatchers.Default) {
                Log.i("prepareThenPlay", "${Thread.currentThread()} start, waiting: ${currentSong!!.id} ${currentSong!!.name}")
                while (true) {
                    if (currentSong!!.isExistInCache()) {
                        break
                    }
                    delay(50)
                }
            }
            Log.i("prepareThenPlay", "back to ${Thread.currentThread()}, ready to play")

            kotlin.runCatching {
                musicPlayer.setDataSource(currentSong!!.toPath())
                musicPlayer.prepare()
            }.onSuccess {
                musicPlayer.isLooping = false
                musicPlayer.setOnCompletionListener { onPlayComplete() }

                CoroutineScope(Dispatchers.Default).launch {
                    titleMessageChannel.send(currentSong!!.name)
                }

                isSongLoad = true

                musicPlayer.start()

                onCompleteCallBack()
            }.onFailure {
                Log.e("prepareThenPlay", "error happened in musicPlayer.setDataSource or musicPlayer.prepare")
                it.printStackTrace()

                onCompleteCallBack()
            }
        }
    }

    fun play(): Boolean {
        if (playlist.isEmpty()) {
            Toast.makeText(getInstance(), "empty playlist", Toast.LENGTH_SHORT).show()
            return false
        }
        if (currentSong == null) { prepareThenPlay() }
        else { musicPlayer.start() }
        Log.i("play", "playing ${currentSong?.id} ${currentSong?.name}")
        return true
    }

    fun pause() {
        musicPlayer.pause()
    }

    fun stop() {
        musicPlayer.stop()
    }

    fun seek(msec: Int, onCompleteCallBack: () -> Unit = {}) {
        musicPlayer.setOnSeekCompleteListener {
            onCompleteCallBack()
        }
        musicPlayer.seekTo(msec)
    }

    fun reset() {
        currentSong = null
        isSongLoad = false
        musicPlayer.reset()
    }

    fun playPrevious(playOnReady: Boolean) {
        pause()

        if (playMode["repeat"] == "one") {
            reset()
            prepareThenPlay { if (!playOnReady) pause() }
            return
        }
        else if (playMode["random"] == "true") {
            Log.w("playPrevious", "not expected using this method when play mode is random")
            val excludeIndex = currentIndex
            while (currentIndex == excludeIndex) { setCurrent((0 until playlist.size).random()) }
            reset()
            prepareThenPlay { if (!playOnReady) pause() }
            return
        }
        else {
            if (currentIndex > 0) { setCurrent(currentIndex - 1) }
            else {
                if (playMode["repeat"] == "on") { setCurrent(playlist.size - 1) }
                else {
                    Toast.makeText(getInstance(), "已经是第一首", Toast.LENGTH_SHORT).show()
                    if (playOnReady) {
                        seek(0)
                        play()
                    }
                    return
                }
            }
            reset()
            prepareThenPlay { if (!playOnReady) pause() }
        }
    }

    fun playNext(playOnReady: Boolean) {
        pause()

        if (playMode["repeat"] == "one") {
            reset()
            prepareThenPlay { if (!playOnReady) pause() }
            return
        }
        else if (playMode["random"] == "true") {
            val excludeIndex = currentIndex
            while (currentIndex == excludeIndex) { setCurrent((0 until playlist.size).random()) }
            reset()
            prepareThenPlay { if (!playOnReady) pause() }
            return
        }
        else {
            if (currentIndex + 1 < playlist.size) { setCurrent(currentIndex + 1) }
            else {
                if (playMode["repeat"] == "on") { setCurrent(0) }
                else {
                    Toast.makeText(getInstance(), "已经是最后一首", Toast.LENGTH_SHORT).show()
                    if (playOnReady) {
                        seek(0)
                        play()
                    }
                    return
                }
            }
            reset()
            prepareThenPlay { if (!playOnReady) pause() }
        }
    }

    fun findSongInPlaylist(song: Song): Int {
        for (i in playlist.indices) {
            if (song.id == playlist[i].id) {
                return i
            }
        }
        return -1
    }

    fun switchToSong(song: Song, save: Boolean = true) {
        reset()
        if (song.id in playlistIdSet) {
            Log.i("switchToSong", "song exist in playlist, play now")
            setCurrent(findSongInPlaylist(song))
            prepareThenPlay()
        }
        else {
            Log.i("switchToSong", "song add to playlist")
            playlist.add(currentIndex + 1, song)
            playlistIdSet.add(song.id)
            setCurrent(currentIndex + 1)
            CoroutineScope(Dispatchers.Default).launch {
                cacheChannel.send(song)
                playlistMessageChannel.send(playlist)
            }
            prepareThenPlay()
            if (save){
                CoroutineScope(Dispatchers.IO).launch {
                    ConfigParser().savePlaylist(playlist)
                }
            }
        }
    }

    fun addSongToPlaylist(song: Song, save: Boolean = true) {
        if (song.id in playlistIdSet) {
            Toast.makeText(getInstance(), "重复添加", Toast.LENGTH_SHORT).show()
            return
        }
        playlist.add(song)
        playlistIdSet.add(song.id)
        CoroutineScope(Dispatchers.Default).launch {
            cacheChannel.send(song)
            playlistMessageChannel.send(playlist)
        }
        if (save){
            CoroutineScope(Dispatchers.IO).launch {
                ConfigParser().savePlaylist(playlist)
            }
        }
    }

    fun delSongInPlaylist(songId: String) {
        for (i in playlist.indices) {
            if (songId == playlist[i].id) {
                if (i == currentIndex) { playNext(musicPlayer.isPlaying) }
                playlist.removeAt(i)
                playlistIdSet.remove(songId)
                if (i <= currentIndex) { setCurrent(currentIndex - 1) }
                else { setCurrent(currentIndex) }
                return
            }
        }
    }

    fun autoCacheSong() {
        // todo: 联网需要修改
        CoroutineScope(Dispatchers.IO).launch {
            Log.i("autoCacheSong", "${Thread.currentThread()} start, waiting for caching song")
            while (true) {
                val song = cacheChannel.receive() ?: break
                Log.i("autoCacheSong", "${song.id} ${song.name} waiting for cache")
                if (song.isExistInCache()) {
                    Log.i("autoCacheSong", "${song.id} ${song.name} cache file already exist")
                }
                else {
                    // Network().fakeGetSong(song.id)
                    Network().downloadSong(song)
                    Log.i("autoCacheSong", "download complete: ${song.id} ${song.name}")
                }
            }
            Log.i("autoCacheSong", "autoCacheSong closed")
        }
    }

    fun stopCacheSong() {
        CoroutineScope(Dispatchers.Main).launch {
            Log.i("stopCacheSong", "request for stopping auto cache song")
            cacheChannel.send(null)
        }
    }

    fun isPlaying(): Boolean {
        return musicPlayer.isPlaying
    }

    fun getProgress(): Int {
        return musicPlayer.currentPosition
    }

    fun getDuration(): Int {
        return musicPlayer.duration
    }

    private fun onPlayComplete() {
        Log.i("onPlayComplete", "play complete")
        playNext(true)
    }
}
