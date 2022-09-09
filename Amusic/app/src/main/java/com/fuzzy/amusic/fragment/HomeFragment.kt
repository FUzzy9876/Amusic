package com.fuzzy.amusic.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuzzy.amusic.MainActivity
import com.fuzzy.amusic.MainApplication.getCsvFileReader
import com.fuzzy.amusic.PlayActivity
import com.fuzzy.amusic.R
import com.fuzzy.amusic.adapter.PlaylistRecycleAdapter
import com.fuzzy.amusic.adapter.SongsRecycleAdapter
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.utils.CsvFileReader
import com.fuzzy.amusic.utils.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment() {
    private val csvFileReader: CsvFileReader = getCsvFileReader()

    var songsList: MutableList<Song> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSongsList()
    }

    private fun initSongsList() {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                csvFileReader.downloadSongsData()
            }
            songsList = csvFileReader.getCsvData(0, 29).toMutableList()

            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.VERTICAL

            val recyclerView = requireView().findViewById<RecyclerView>(R.id.all_songs_list)
            recyclerView.layoutManager = layoutManager
            recyclerView.adapter = SongsRecycleAdapter(songsList) { loadMoreSongs() }
        }
    }

    private fun loadMoreSongs(): List<Song> {
        val moreSongs = csvFileReader.getCsvData(songsList.size, songsList.size + 30 - 1)
        songsList.addAll(moreSongs)
        return moreSongs
    }

    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

}
