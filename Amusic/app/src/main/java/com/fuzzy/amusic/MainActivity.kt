package com.fuzzy.amusic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.fragment.NavHostFragment
import com.fuzzy.amusic.MainApplication.getMusicService
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.utils.ConfigParser
import com.fuzzy.amusic.utils.CsvFileReader
import com.fuzzy.amusic.utils.Network
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    val musicService = getMusicService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initMusicService()

        // 悬浮按钮
        findViewById<FloatingActionButton>(R.id.switch_to_player).setOnClickListener {
            startActivity(Intent(this, PlayActivity::class.java))
        }

        // fragment导航
        initNav()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun initMusicService() {
        musicService.autoCacheSong()

        for (song in ConfigParser().loadPlaylist()) {
            musicService.addSongToPlaylist(song, false)
        }
    }

    private fun initNav() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment.navController

        val tabBar = findViewById<TabLayout>(R.id.tab_bar)
        val tabHome: TabLayout.Tab? = tabBar.getTabAt(0)
        tabHome!!.view.setOnClickListener { navController.navigate(R.id.homeFragment) }
        val tabSearch: TabLayout.Tab? = tabBar.getTabAt(1)
        tabSearch!!.view.setOnClickListener { navController.navigate(R.id.searchFragment) }
    }
}
