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

class SongsRecycleAdapter(songsList: List<Song>, addSongsMethod: () -> List<Song>) :
    RecyclerView.Adapter<SongsRecycleAdapter.ViewHolder>() {
    private val songs: MutableList<Song> = songsList.toMutableList()
    private val musicService: MusicService = getMusicService()
    private val loadSongs = addSongsMethod

    class ViewHolder(itemView: View,val viewType: Int) : RecyclerView.ViewHolder(itemView) {
        val item = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_songs_list, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_more_button, parent, false)
        }
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.viewType == 0) {
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
                musicService.switchToSong(song)
            }
            addButton.setOnClickListener {
                musicService.addSongToPlaylist(song)
            }
        }
        else {
            val loadButton: Button = holder.item.findViewById(R.id.load_more)
            loadButton.setOnClickListener {
                addItemsAtTail()
            }
        }
    }

    override fun getItemCount() = songs.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position < songs.size) { 0 } else { 1 }
    }

    private fun addItemsAtTail() {
        val startIndex = songs.size
        songs.addAll(loadSongs())
        val endIndex = songs.size
        notifyItemRangeInserted(startIndex, endIndex - startIndex)
    }
}