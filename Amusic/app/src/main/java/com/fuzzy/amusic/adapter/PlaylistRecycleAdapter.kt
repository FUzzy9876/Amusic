package com.fuzzy.amusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.fuzzy.amusic.MainApplication.getMusicService
import com.fuzzy.amusic.utils.Tools.prettyTime
import com.fuzzy.amusic.R
import com.fuzzy.amusic.database.Song

class PlaylistRecycleAdapter(private val playlist: MutableList<Song>, private val currentIndex: Int) :
    RecyclerView.Adapter<PlaylistRecycleAdapter.ViewHolder>() {
    private val musicService = getMusicService()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layout: ConstraintLayout = itemView.findViewById(R.id.item_layout)
        val title: TextView = itemView.findViewById(R.id.item_title)
        val artist: TextView = itemView.findViewById(R.id.item_artist)
        val date: TextView = itemView.findViewById(R.id.item_date)
        val length: TextView = itemView.findViewById(R.id.item_length)
        val button: ImageButton = itemView.findViewById(R.id.item_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = playlist[position]

        holder.title.text = song.name
        holder.artist.text = song.artists
        holder.date.text = song.date
        holder.length.text = prettyTime(song.length)

        if (position == currentIndex) { holder.title.paint.isFakeBoldText = true }
        else {
            holder.title.setOnClickListener {
                musicService.switchToSong(song, true)
            }
        }

        holder.button.setOnClickListener {
            musicService.delFromPlaylist(song)
        }
    }

    override fun getItemCount() = playlist.size
}