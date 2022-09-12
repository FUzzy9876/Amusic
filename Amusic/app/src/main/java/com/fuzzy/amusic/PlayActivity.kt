package com.fuzzy.amusic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuzzy.amusic.MainApplication.getMusicService
import com.fuzzy.amusic.utils.MusicService
import com.fuzzy.amusic.adapter.PlaylistRecycleAdapter
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.utils.Tools.prettyTime
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*

class PlayActivity : AppCompatActivity() {
    val musicService: MusicService = getMusicService()

    private var running: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        running = true

        musicService.setPlayActivity(this)

        initToolbar()
        autoSeekBar()
        initButtons()
        initRecycleView(false)
    }

    override fun onDestroy() {
        running = false

        musicService.setPlayActivity(null)

        super.onDestroy()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        toolbar.title = musicService.currentSong?.name
        toolbar.subtitle = musicService.currentSong?.artists
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }
    
    private fun setToolbar(title: String?, subTitle: String?) {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        Log.d("setToolbar", "$title, $subTitle")
        toolbar.title = title
        toolbar.subtitle = subTitle
    }

    private fun initRecycleView(isShow: Boolean) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL

        val recyclerView: RecyclerView = findViewById(R.id.play_list)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = PlaylistRecycleAdapter(musicService.playlist, musicService.currentIndex)
        recyclerView.isVisible = isShow
    }

    private fun updateRecycleView() {
        findViewById<RecyclerView>(R.id.play_list).adapter = PlaylistRecycleAdapter(musicService.playlist, musicService.currentIndex)
    }

    private fun initButtons() {
        // 开始按钮
        val startButton: FloatingActionButton = findViewById(R.id.start_button)
        if (musicService.isPlaying) {
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
        if (musicService.mode["random"] == "true") {
            findViewById<ImageButton>(R.id.button_playmode_random).setImageResource(R.drawable.music_random_on)
        }
        else {
            findViewById<ImageButton>(R.id.button_playmode_random).setImageResource(R.drawable.music_random_off)
        }

        when (musicService.mode["repeat"]) {
            "one" -> { findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_one) }
            "on" -> { findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_on) }
            "off" -> { findViewById<ImageButton>(R.id.button_playmode_repeat).setImageResource(R.drawable.music_repeat_off) }
        }

    }

    private fun onPlayButtonClick() {
        musicService.play()
    }

    private fun onPauseButtonClick() {
        musicService.pause()
    }

    private fun onPrevButtonClick() {
        if (musicService.playlist.isEmpty()) { return }

        musicService.playPrev(musicService.isPlaying)
        resetSeekbar()
    }

    private fun onNextButtonClick() {
        if (musicService.playlist.isEmpty()) { return }

        musicService.playNext(musicService.isPlaying)
        resetSeekbar()
    }

    private fun autoSeekBar() {
        var update = true
        val seekBar: SeekBar = findViewById(R.id.seek_bar)
        val currentTime: TextView = findViewById(R.id.time_current)
        val totalTime: TextView = findViewById(R.id.time_total)

        if (musicService.isLoad) {
            val total: Int = musicService.duration
            val current: Int = musicService.currentPosition

            seekBar.max = total
            seekBar.progress = current

            totalTime.text = prettyTime(total)
            currentTime.text = prettyTime(current)
        }
        else {
            resetSeekbar()
        }

        CoroutineScope(Dispatchers.Main).launch {
            Log.d("PlayActivity / upgradeSeekBar", "running")
            while (running) {
                if (update && musicService.isPlaying) {
                    val total = musicService.duration
                    val current = musicService.currentPosition

                    seekBar.max = total
                    seekBar.progress = current

                    totalTime.text = prettyTime(total)
                    currentTime.text = prettyTime(current)
                }
                delay(500)
            }
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onStartTrackingTouch(seekbar: SeekBar?) {
                Log.i("PlayActivity / seekbar", "seekbar start tracking, will not update automatically")
                update = false
            }

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (musicService.isLoad && seekbar != null) {
                    Log.i("PlayActivity / seekbar", "seekbar stop tracking, current progress is ${seekbar.progress}")
                    musicService.seek(seekbar.progress) { update = true }
                }
                else { resetSeekbar() }
            }

            override fun onProgressChanged(seekbar: SeekBar?, value: Int, p2: Boolean) { }
        })
    }

    private fun resetSeekbar() {
        val seekBar: SeekBar = findViewById(R.id.seek_bar)
        seekBar.max = 1
        seekBar.progress = 0

        findViewById<TextView>(R.id.time_current).text = prettyTime(0)
        findViewById<TextView>(R.id.time_total).text = prettyTime(0)
    }

    fun onRandomButtonClick(view: View?) {
        if (musicService.mode["random"] == "true") {
            musicService.mode["random"] = "false"
            findViewById<ImageButton>(R.id.button_playmode_random)?.setImageResource(R.drawable.music_random_off)
        }
        else {
            musicService.mode["random"] = "true"
            findViewById<ImageButton>(R.id.button_playmode_random)?.setImageResource(R.drawable.music_random_on)
        }
    }

    fun onRepeatButtonClick(view: View?) {
        when (musicService.mode["repeat"]) {
            "off" -> {
                musicService.mode["repeat"] = "on"
                findViewById<ImageButton>(R.id.button_playmode_repeat)?.setImageResource(R.drawable.music_repeat_on)
            }
            "on" -> {
                musicService.mode["repeat"] = "one"
                findViewById<ImageButton>(R.id.button_playmode_repeat)?.setImageResource(R.drawable.music_repeat_one)
            }
            "one" -> {
                musicService.mode["repeat"] = "off"
                findViewById<ImageButton>(R.id.button_playmode_repeat)?.setImageResource(R.drawable.music_repeat_off)
            }
        }
    }

    fun onPlaylistButtonClick(view: View?) {
        val playlistButton: ImageButton =  findViewById(R.id.button_playlist)
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


    fun onPlaylistChange() {
        updateRecycleView()
    }

    fun onSongChanged(currentIndex: Int, currentSong: Song?) {
        resetSeekbar()

        setToolbar(currentSong?.name, currentSong?.artists)
        updateRecycleView()
    }

    fun onPlayerStart() {
        findViewById<FloatingActionButton>(R.id.start_button).setImageResource(R.drawable.music_pause)
        findViewById<FloatingActionButton>(R.id.start_button).setOnClickListener {
            onPauseButtonClick()
        }
    }

    fun onPlayerPause() {
        findViewById<FloatingActionButton>(R.id.start_button).setImageResource(R.drawable.music_play)
        findViewById<FloatingActionButton>(R.id.start_button).setOnClickListener {
            onPlayButtonClick()
        }
    }

    fun onPlayerStop() {
        findViewById<FloatingActionButton>(R.id.start_button).setImageResource(R.drawable.music_play)
        findViewById<FloatingActionButton>(R.id.start_button).setOnClickListener {
            onPlayButtonClick()
        }
    }

    fun onSongLoad(currentIndex: Int, currentSong: Song?) {
        resetSeekbar()

        setToolbar(currentSong?.name, currentSong?.artists)
        updateRecycleView()
    }

    fun onSongUnload() {
        resetSeekbar()

        setToolbar("", "")
        updateRecycleView()
    }
}
