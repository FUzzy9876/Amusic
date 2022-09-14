package com.fuzzy.amusic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fuzzy.amusic.MainApplication.getMusicService
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.fragment.HomeFragment
import com.fuzzy.amusic.utils.ConfigParser
import com.fuzzy.amusic.utils.CsvFileReader
import com.fuzzy.amusic.utils.MusicService
import com.fuzzy.amusic.utils.Network
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class MainActivity : AppCompatActivity() {
    private val musicService: MusicService = getMusicService()
    private var running: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        running = true

        initMusicService()

        initButton()

        // fragment导航
        initNav()

        initLoadingIcon()
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun initMusicService() {
        for (song in ConfigParser().loadPlaylist()) {
            musicService.addToPlaylist(song, save=false)
        }
    }

    private fun initNav() {
        val navHostFragment: NavHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController: NavController = navHostFragment.navController

        val tabBar: TabLayout = findViewById(R.id.tab_bar)
        val tabHome: TabLayout.Tab? = tabBar.getTabAt(0)
        tabHome!!.view.setOnClickListener { navController.navigate(R.id.homeFragment) }
        val tabSearch: TabLayout.Tab? = tabBar.getTabAt(1)
        tabSearch!!.view.setOnClickListener { navController.navigate(R.id.searchFragment) }
    }

    private fun initLoadingIcon() {
        val loadingIcon: ImageView = findViewById(R.id.loading_icon)
        loadingIcon.isVisible = false

        CoroutineScope(Dispatchers.Default).launch {
            val alphaAnimation: AlphaAnimation = AlphaAnimation(0.2f, 0.8f)
            alphaAnimation.duration = 2000
            alphaAnimation.repeatCount = Animation.INFINITE

            while (running) {
                if (!loadingIcon.isVisible && musicService.cachingList.size > 0) {
                    withContext(Dispatchers.Main) {
                        loadingIcon.isVisible = true
                        loadingIcon.startAnimation(alphaAnimation)
                    }
                }
                if (loadingIcon.isVisible && musicService.cachingList.size == 0) {
                    withContext(Dispatchers.Main) {
                        loadingIcon.isVisible = false
                        loadingIcon.clearAnimation()
                    }
                }
                delay(100)
            }
        }
    }

    private fun initButton() {
        // 悬浮按钮
        findViewById<FloatingActionButton>(R.id.switch_to_player).setOnClickListener {
            startActivity(Intent(this, PlayActivity::class.java))
        }
        // logo按钮
        findViewById<TextView>(R.id.app_title).setOnClickListener {
            onTopBarClicked()
        }
    }

    private fun onTopBarClicked() {
        val navHostFragment = this.supportFragmentManager.fragments.first() as NavHostFragment
        navHostFragment.childFragmentManager.fragments.forEach {
            if (HomeFragment::class.java.isAssignableFrom(it.javaClass)) {
                val fragment: HomeFragment = it as HomeFragment
                fragment.updateSongsList()
            }
        }
    }
}
