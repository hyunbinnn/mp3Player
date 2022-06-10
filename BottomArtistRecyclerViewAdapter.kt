package com.example.musicplayer_220603

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer_220603.databinding.BottomItemViewBinding

class BottomArtistRecyclerViewAdapter(val context: Context?, val musicList: MutableList<Music>): RecyclerView.Adapter<BottomArtistRecyclerViewAdapter.ViewHolder>() {
    val artistFilteredList = musicList.run {
        this.distinctBy { it.artist }
    }.run{
        this.filter { it.artist != "null" }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BottomArtistRecyclerViewAdapter.ViewHolder {
        val binding = BottomItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomArtistRecyclerViewAdapter.ViewHolder, position: Int) {
        val binding = holder.binding

        artistFilteredList.filter { !it.artist.isNullOrBlank() }

        binding.textView.text = artistFilteredList[position].artist
        binding.root.setOnClickListener {
            artistFilteredList[position].artist?.let {
                (context as MainActivity).getFilteredMusicList(it, Type.ARTIST)
            }
        }
    }

    override fun getItemCount(): Int {
        return artistFilteredList.size
    }

    inner class ViewHolder(val binding: BottomItemViewBinding): RecyclerView.ViewHolder(binding.root)
}