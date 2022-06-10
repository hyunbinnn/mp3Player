package com.example.musicplayer_220603

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer_220603.databinding.BottomItemViewBinding

class BottomGenreRecyclerViewAdapter(val context: Context?, val musicList: MutableList<Music>): RecyclerView.Adapter<BottomGenreRecyclerViewAdapter.ViewHolder>() {
    val genreFilteredList = musicList.run {
        this.distinctBy { it.genre }
    }.run{
        this.filter { it.genre != "null" }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BottomGenreRecyclerViewAdapter.ViewHolder {
        val binding = BottomItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BottomGenreRecyclerViewAdapter.ViewHolder, position: Int) {
        val binding = holder.binding

        binding.textView.text = genreFilteredList[position].genre
        binding.root.setOnClickListener {
            genreFilteredList[position].genre?.let {
                (context as MainActivity).getFilteredMusicList(it, Type.GENRE)
            }
        }
    }

    override fun getItemCount(): Int {
        return genreFilteredList.size
    }

    inner class ViewHolder(val binding: BottomItemViewBinding): RecyclerView.ViewHolder(binding.root)
}