package com.fuzzy.amusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuzzy.amusic.MainApplication.getMusicService
import com.fuzzy.amusic.utils.MusicService
import com.fuzzy.amusic.utils.Network
import com.fuzzy.amusic.adapter.PlaylistRecycleAdapter
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.utils.Tools.prettyTime
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class PlayActivity : AppCompatActivity() {
    val network: Network = Network()
    val musicService: MusicService = getMusicService()
    var running = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        initTitle()
        autoSeekBar()
        initButtons()
        initRecycleView(false)
    }

    override fun onDestroy() {
        stopUpgradeTitle()
        stopUpgradeRecycleView()
        running = false

        super.onDestroy()
    }

    private fun initTitle() {
        val songTitle = findViewById<TextView>(R.id.song_title)
        var currentTitle: String? = musicService.currentSong?.name
        songTitle.text = currentTitle

        val titleMessageChannel = musicService.titleMessageChannel
        CoroutineScope(Dispatchers.Main).launch {
            Log.i("initTitle(upgrade Title)", "running on ${Thread.currentThread()}")
            while (running) {
                currentTitle = titleMessageChannel.receive() ?: break
                Log.i("upgrade Title", "receive message: $currentTitle")
                songTitle.text = currentTitle
            }
            Log.i("initTitle", "close auto upgrade")
        }
    }

    private fun stopUpgradeTitle() {
        CoroutineScope(Dispatchers.Default).launch {
            musicService.titleMessageChannel.send(null)
        }
    }

    private fun initRecycleView(isShow: Boolean) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        val recyclerView = findViewById<RecyclerView>(R.id.play_list)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = PlaylistRecycleAdapter(musicService.playlist, musicService.currentIndex)

        recyclerView.bringToFront()
        recyclerView.isVisible = isShow

        val playlistMessageChannel = musicService.playlistMessageChannel
        CoroutineScope(Dispatchers.Main).launch {
            Log.i("initRecycleView(upgrade RecycleView)", "running on ${Thread.currentThread()}")
            while (running) {
                val newPlaylist = playlistMessageChannel.receive() ?: break
                Log.i("upgrade RecycleView", "receive message: $newPlaylist")
                recyclerView.adapter = PlaylistRecycleAdapter(newPlaylist, musicService.currentIndex)
            }
            Log.i("upgrade RecycleView", "close auto upgrade")
        }
    }

    private fun stopUpgradeRecycleView() {
        CoroutineScope(Dispatchers.Default).launch {
            musicService.playlistMessageChannel.send(null)
        }
    }

    private fun initButtons() {
        // 开始按钮
        val startButton = findViewById<FloatingActionButton>(R.id.start_button)
        if (musicService.isPlaying()) {
            startButton.setImageResource(R.drawable.music_pause)
            startButton.setOnClickListener { onPauseButtonClick() }
        }
        else {
            startButton.setImageResource(R.drawable.music_play)
            startButton.setOnClickListener { onPlayButtonClick() }
        }

        // 上一曲、下一曲
        findViewById<FloatingActionButton>(R.id.prev_button).setOnClickListener { onPrevButtonClick() }
        findViewById<FloatingActionButton>(R.id.next_button).setOnClickListener { onNextButtonClick() }

        // 播放模式
        if (musicService.playMode["random"] == "true") {
            findViewById<ImageButton>(R.id.button_playmode_random).setImageResource(R.drawable.music_random_on)
        }
        else {
            findViewById<ImageButton>(R.id.button_playmode_random).setImageResource(R.drawable.music_random_off)
        }

        when (musicService.playMode["repeat"]) {
            "one" -> { findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_one) }
            "on" -> { findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_on) }
            "off" -> { findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_off) }
        }

    }

    private fun onPlayButtonClick() {
        if (!musicService.play()) {
            return
        }

        findViewById<FloatingActionButton>(R.id.start_button).setImageResource(R.drawable.music_pause)
        findViewById<FloatingActionButton>(R.id.start_button).setOnClickListener {
            onPauseButtonClick()
        }
    }

    private fun onPauseButtonClick() {
        musicService.pause()

        findViewById<FloatingActionButton>(R.id.start_button).setImageResource(R.drawable.music_play)
        findViewById<FloatingActionButton>(R.id.start_button).setOnClickListener {
            onPlayButtonClick()
        }
    }

    private fun onPrevButtonClick() {
        if (musicService.playlist.isEmpty()) { return }

        musicService.playPrevious(musicService.isPlaying())
        resetSeekbar()
    }

    private fun onNextButtonClick() {
        if (musicService.playlist.isEmpty()) { return }

        musicService.playNext(musicService.isPlaying())
        resetSeekbar()
    }

    private fun autoSeekBar() {
        var update = true
        val seekBar = findViewById<SeekBar>(R.id.seek_bar)
        val currentTime = findViewById<TextView>(R.id.time_current)
        val totalTime = findViewById<TextView>(R.id.time_total)

        if (musicService.isSongLoad) {
            val current = musicService.getProgress()
            val total = musicService.getDuration()

            seekBar.max = total
            seekBar.progress = current

            totalTime.text = prettyTime(total)
            currentTime.text = prettyTime(current)
        }
        else {
            resetSeekbar()
        }

        CoroutineScope(Dispatchers.Main).launch {
            Log.d("upgradeSeekBar(autoSeekBar)", "running")
            while (running) {
                if (update && musicService.isPlaying()) {
                    val current = musicService.getProgress()
                    val total = musicService.getDuration()

                    seekBar.max = total
                    seekBar.progress = current

                    totalTime.text = prettyTime(total)
                    currentTime.text = prettyTime(current)
                }
                delay(1000)
            }
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(seekbar: SeekBar?) {
                update = false
            }

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (musicService.isSongLoad && seekbar != null) {
                    musicService.seek(seekbar.progress) { update = true }
                }
                else { resetSeekbar() }
            }

            override fun onProgressChanged(seekbar: SeekBar?, value: Int, p2: Boolean) {
            }
        })
    }

    private fun resetSeekbar() {
        val seekBar = findViewById<SeekBar>(R.id.seek_bar)
        seekBar.max = 1
        seekBar.progress = 0

        findViewById<TextView>(R.id.time_current).text = prettyTime(0)
        findViewById<TextView>(R.id.time_total).text = prettyTime(0)
    }

    fun onRandomButtonClick(view: View?) {
        if (musicService.playMode["random"] == "true") {
            musicService.playMode["random"] = "false"
            findViewById<ImageButton>(R.id.button_playmode_random).setImageResource(R.drawable.music_random_off)
        }
        else {
            musicService.playMode["random"] = "true"
            findViewById<ImageButton>(R.id.button_playmode_random).setImageResource(R.drawable.music_random_on)
        }
    }

    fun onRepeatButtonClick(view: View?) {
        when (musicService.playMode["repeat"]) {
            "off" -> {
                musicService.playMode["repeat"] = "on"
                findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_on)
            }
            "on" -> {
                musicService.playMode["repeat"] = "one"
                findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_one)
            }
            "one" -> {
                musicService.playMode["repeat"] = "off"
                findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_off)
            }
        }
    }

    fun onPlaylistButtonClick(view: View?) {
        val playlistButton =  findViewById<ImageButton>(R.id.button_playlist)
        val playlistView = findViewById<RecyclerView>(R.id.play_list)
        if (playlistView.isVisible) {
            playlistView.isVisible = false
            playlistButton.setImageResource(R.drawable.music_playlist_off)
        }
        else {
            playlistView.isVisible = true
            playlistButton.setImageResource(R.drawable.music_playlist_on)
        }
    }

    fun testOnAdd1(view: View?) {
        musicService.addSongToPlaylist(Song("001326", "我们快出发"))
    }

    fun testOnAdd2(view: View?) {
        musicService.addSongToPlaylist(Song("001333", "除夕"))
    }
}
