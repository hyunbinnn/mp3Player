package com.example.musicplayer_220603

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer_220603.databinding.MainItemViewBinding

class MainListAdapter(val context: Context, val musicList: MutableList<Music>):
    RecyclerView.Adapter<MainListAdapter.ViewHolder>() {
    val mainActivity: MainActivity = (context as MainActivity)
    val dbHelper: DBHelper by lazy {
        DBHelper(context)
    }

    val ALBUM_IMAGE_SIZE = 150

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainListAdapter.ViewHolder {
        val binding = MainItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainListAdapter.ViewHolder, position: Int) {
        val binding = holder.binding

        binding.tvRVArtist.text = musicList[holder.adapterPosition].artist
        binding.tvRVTitle.text = musicList[holder.adapterPosition].title

        when(musicList[holder.adapterPosition].favorite){
            0 -> {
                holder.binding.ivRVFavorite.setImageResource(R.drawable.ic_baseline_star_border_24)
            }

            1 -> {
                holder.binding.ivRVFavorite.setImageResource(R.drawable.ic_baseline_star_24)
            }
        }

        val bitmap: Bitmap? = musicList[holder.adapterPosition].getAlbumImage(context, ALBUM_IMAGE_SIZE)
        if (bitmap != null){
            holder.binding.ivRVAlbumImage.setImageBitmap(bitmap)
        } else {
            holder.binding.ivRVAlbumImage.setImageResource(R.drawable.ic_baseline_music_note_24)
        }

        binding.ivRVFavorite.setOnClickListener {
            when(musicList[position].favorite){
                0 -> {
                    musicList[position].favorite = 1

                    if (dbHelper.updateFavorite(musicList[position])){
                        Toast.makeText(context, "Successfully added to your favorites", Toast.LENGTH_SHORT).show()
                    }

                    notifyItemChanged(position)
                }

                1 -> {
                    musicList[position].favorite = 0

                    if (dbHelper.updateFavorite(musicList[position])){
                        Toast.makeText(context, "Removed from your favorites", Toast.LENGTH_SHORT).show()
                        if (mainActivity.typeOfList == Type.FAVORITE){
                            Log.d("Log_debug", "list type = ${mainActivity.typeOfList}")
                            musicList.remove(musicList[position])
                        }
                    }
                    notifyDataSetChanged()
                }
            }
        }

        binding.root.setOnClickListener {
            mainActivity.nowPlaying(musicList[position])
        }
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    inner class ViewHolder(val binding: MainItemViewBinding): RecyclerView.ViewHolder(binding.root)

}