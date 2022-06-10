package com.example.musicplayer_220603

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

class ListDecoration(val context: Context): RecyclerView.ItemDecoration() {

    //각 아이템뷰마다 적용
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val index = parent.getChildAdapterPosition(view)
        if(index % 3 == 0){
            outRect.set(10, 10, 10, 60)
        } else {
            outRect.set(10, 10, 10, 0)
        }

        view.setBackgroundColor(Color.GRAY)
        ViewCompat.setElevation(view, 10.0f)

    }
}