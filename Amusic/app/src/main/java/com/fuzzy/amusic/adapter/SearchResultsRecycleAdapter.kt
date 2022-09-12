package com.fuzzy.amusic.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuzzy.amusic.MainApplication.getMusicService
import com.fuzzy.amusic.R
import com.fuzzy.amusic.database.Song
import com.fuzzy.amusic.utils.MusicService
import com.fuzzy.amusic.utils.Tools

class SearchResultsRecycleAdapter(songsList: List<Song>) :
    RecyclerView.Adapter<SearchResultsRecycleAdapter.ViewHolder>() {
    private val songs: MutableList<Song> = songsList.toMutableList()
    private val musicService: MusicService = getMusicService()

    class ViewHolder(itemView: View,val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        val item = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_songs_list, parent, false), viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title: TextView = holder.item.findViewById(R.id.item_title)
        val artist: TextView = holder.item.findViewById(R.id.item_artist)
        val date: TextView = holder.item.findViewById(R.id.item_date)
        val length: TextView = holder.item.findViewById(R.id.item_length)
        val addButton: ImageButton = holder.item.findViewById(R.id.item_button)

        val song = songs[position]

        title.text = song.name
        artist.text = song.artists
        date.text = song.date
        length.text = Tools.prettyTime(song.length)

        title.setOnClickListener {
            musicService.switchToSong(song, true)
        }
        addButton.setOnClickListener {
            musicService.addToPlaylist(song)
        }
    }

    override fun getItemCount(): Int = songs.size
}