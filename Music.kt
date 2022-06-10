package com.example.musicplayer_220603

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.IOException

data class Music(val id: String, val title: String?, val artist: String?, val albumId: String?, val genre: String?, val duration: Int?, var favorite: Int = 0) {

    //contentresolver를 이용하여 앨범정보를 가져오기 위한 경로 Uri
    fun getAlbumUri(): Uri {
        return Uri.parse("content://media/external/audio/albumart/"+albumId)
    }

    //음악정보를 가져오기 위한 경로 Uri
    fun getMusicUri(): Uri {
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

    //해당 음악 비트맵 이미지 만들기
    fun getAlbumImage(context: Context, albumImageSize: Int): Bitmap?{
        val contentResolver: ContentResolver = context.getContentResolver()
        //앨범 경로
        val uri = getAlbumUri()
        //앨범의 정보를 저장하기 위해
        val options = BitmapFactory.Options()

        if(uri != null){
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            try {
                //외부파일에 있는 이미지파일을 가져오기 위한 스트림
                parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                var bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor!!.fileDescriptor, null, options)

                //비트맵을 가져와서 사이즈 결정(원본 이미지 사이가 원하는 사이즈가 아닐 경우 원하는 사이즈로 변환)
                if(bitmap != null){
                    if(options.outHeight !== albumImageSize || options.outWidth !== albumImageSize){
                        val tempBitmap = Bitmap.createScaledBitmap(bitmap, albumImageSize, albumImageSize, true)
                        bitmap.recycle()
                        bitmap = tempBitmap
                    }
                }
                return bitmap
            } catch (e: Exception){
                Log.d("Log_debug", "getAlbumImage() ${e.printStackTrace()}")
            } finally {
                try {
                    parcelFileDescriptor?.close()
                } catch (e: IOException){
                    Log.d("Log_debug", "parcelFileDescriptor?.close() ${e.printStackTrace()}")
                }
            }
        }
        return null
    }
}