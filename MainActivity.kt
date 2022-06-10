package com.example.musicplayer_220603

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.SearchView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer_220603.databinding.ActivityMainBinding
import com.example.musicplayer_220603.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val ALBUM_IMAGE_SIZE_LARGE = 240
    val ALBUM_IMAGE_SIZE_SMALL = 100

    val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    val bottomSheetBinding: BottomSheetBinding by lazy {
        binding.bottomSheet
    }

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

    lateinit var toggle: ActionBarDrawerToggle

    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    val REQUEST_READ = 100

    val dbHelper: DBHelper by lazy {
        DBHelper(this)
    }

    lateinit var adapter: MainListAdapter

    var playMusicList: MutableList<Music> = mutableListOf()
    var displayMusicList: MutableList<Music> = mutableListOf()
    var searchedMusicList: MutableList<Music> = mutableListOf()

    var typeOfList = Type.ALL

    var isPlaying = false

    //미디어플레이어
    private var mediaPlayer: MediaPlayer? = null

    //음악정보
    private var currentMusic: Music? = null

    //Coroutine scope
    private var playerJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        //Bottom Sheet setting
        sheetBehavior = BottomSheetBehavior.from<ConstraintLayout>(binding.bottomSheet.root)
        val insets =
            windowManager.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        val constraintLayoutParams = binding.recyclerView.layoutParams
        val toolbarParams = binding.toolbar.layoutParams
        constraintLayoutParams.height =
            windowManager.currentWindowMetrics.bounds.height() - sheetBehavior.peekHeight - insets.top - insets.bottom - toolbarParams.height

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheetBinding.bottomSheetSmall.root.alpha = 1 - slideOffset
                if (bottomSheetBinding.bottomSheetSmall.root.alpha == 0f) {
                    bottomSheetBinding.bottomSheetSmall.root.visibility = View.INVISIBLE
                } else {
                    bottomSheetBinding.bottomSheetSmall.root.visibility = View.VISIBLE
                }

                bottomSheetBinding.bottomSheet.alpha = 0 + slideOffset
                if (bottomSheetBinding.bottomSheet.alpha == 0f) {
                    bottomSheetBinding.bottomSheet.visibility = View.INVISIBLE
                } else {
                    bottomSheetBinding.bottomSheet.visibility = View.VISIBLE
                }
            }
        })

        bottomSheetBinding.bottomSheetSmall.root.setOnClickListener {
            sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        //Toolbar section
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        if (isPermitted()) {
            startProcess()
        } else {
            ActivityCompat.requestPermissions(this, permission, REQUEST_READ)
        }

        bottomSheetBinding.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            //시크바를 터치하고 이동할 때
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {//user가 직접 interaction할 때
                    //progress 만큼 노래 재생시간 이동
                    mediaPlayer?.seekTo(progress)
                    bottomSheetBinding.tvDurationStart.text =
                        SimpleDateFormat("mm:ss").format(progress)
                }
            }

            //시크바를 터치할 때
            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            //시크바 터치가 끝났을 때
            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_READ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startProcess()
            }
        } else {
            Toast.makeText(this, "권한 요청을 승인하여야 이 앱을 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return
        }
        super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.menuSongs -> {
                binding.collapssingToolbarLayout.title = "Songs"
                getFilteredMusicList(null, Type.ALL)
                binding.drawerLayout.closeDrawers()
            }
            R.id.menuArtist -> {
                binding.collapssingToolbarLayout.title = "Artist"
                val bottomSheetDialog = BottomSheetDialog(Type.ARTIST)
                bottomSheetDialog.show(supportFragmentManager, bottomSheetDialog.TAG)
                binding.drawerLayout.closeDrawers()
            }
            R.id.menuGenre -> {
                binding.collapssingToolbarLayout.title = "Genre"
                val bottomSheetDialog = BottomSheetDialog(Type.GENRE)
                bottomSheetDialog.show(supportFragmentManager, bottomSheetDialog.TAG)
                binding.drawerLayout.closeDrawers()
            }
            R.id.menuFavorite -> {
                binding.collapssingToolbarLayout.title = "Favorite"
                getFilteredMusicList(null, Type.FAVORITE)
                binding.drawerLayout.closeDrawers()
            }
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchMenu = menu?.findItem(R.id.search_item)
        val searchView = searchMenu?.actionView as SearchView
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            //검색 버튼 클릭 시 검색 결과만 화면에 출력
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            //검색 텍스트 변화시 검색 결과만 화면에 출력
            override fun onQueryTextChange(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                        searchedMusicList = dbHelper.selectMusic(query)
                        binding.recyclerView.adapter = MainListAdapter(this@MainActivity, searchedMusicList)
                } else {
                    //검색 텍스트가 비었을 경우 전체 데이터 출력
                    binding.recyclerView.adapter = MainListAdapter(this@MainActivity, displayMusicList)
                    searchedMusicList.clear()
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }


    //외부 파일 읽기 권한 승인 여부
    fun isPermitted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission[0]
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startProcess() {
        displayMusicList = dbHelper.selectMusicAll()

        if (displayMusicList == null || displayMusicList.size <= 0) {
            displayMusicList = getMusicListFromMobile()
            for (i in 0 until displayMusicList!!.size) {
                val music = displayMusicList!![i]
                if (dbHelper.insertMusic(music) == false) {
                    Log.d("Log_debug", "삽입 오류 ${music}")
                }
            }
        }
        Log.d("Log_debug", "${displayMusicList}")

        val layoutManager = LinearLayoutManager(this)
//        val itemDecoration = ListDecoration(this)
        adapter = MainListAdapter(this, displayMusicList!!)
        binding.recyclerView.adapter = adapter
//        binding.recyclerView.addItemDecoration(itemDecoration)
        binding.recyclerView.layoutManager = layoutManager
    }

    fun getMusicListFromMobile(): MutableList<Music> {
        //외부 파일의 음악 정보 주소
        val listUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        //음원 정보 컬럼
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,     //음악 아이디
            MediaStore.Audio.Media.TITLE,    //음악 타이틀
            MediaStore.Audio.Media.ARTIST,    //가수
            MediaStore.Audio.Media.ALBUM_ID,    //앨범 이미지
            MediaStore.Audio.Media.GENRE,    //장르
            MediaStore.Audio.Media.DURATION,    //곡 길이
        )

        //contentResolver
        val cursor = contentResolver.query(listUri, proj, null, null, null)
        val musicList = mutableListOf<Music>()

        while (cursor?.moveToNext() == true) {
            val id = cursor.getString(0)
            val title = cursor.getString(1).replace("'", "")
            var artist = cursor.getString(2).replace("'", "")
            var albumId = cursor.getString(3)
            var genre = cursor.getString(4)
            var duration = cursor.getInt(5)

            val music = Music(id, title, artist, albumId, genre, duration, 0)

            musicList.add(music)
        }
        cursor?.close()

        return musicList
    }

    fun getFilteredMusicList(filter: String?, type: Type) {
        displayMusicList = dbHelper.selectFilter(filter, type)!!
        binding.recyclerView.adapter = MainListAdapter(this, displayMusicList)
        typeOfList = type
    }

    fun nowPlaying(music: Music) {
        musicStop()

        if (searchedMusicList.isEmpty()) {
            playMusicList = displayMusicList
            currentMusic = displayMusicList[displayMusicList.indexOf(music)]
        } else {
            playMusicList = searchedMusicList
            currentMusic = playMusicList[playMusicList.indexOf(music)]
        }
        bottomSheetUpdate(music)

        mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())

        if (mediaPlayer?.isPlaying == false){
            musicStart()
        }
    }

    fun bottomSheetUpdate(music: Music){
        val bitmapLarge: Bitmap? = music.getAlbumImage(this, ALBUM_IMAGE_SIZE_LARGE)
        val bitmapSmall: Bitmap? = music.getAlbumImage(this, ALBUM_IMAGE_SIZE_SMALL)

        if (bitmapLarge != null && bitmapSmall != null) {
            bottomSheetBinding.ivAlbumImageLarge.setImageBitmap(bitmapLarge)
            bottomSheetBinding.bottomSheetSmall.ivAlbumImageSmall.setImageBitmap(bitmapSmall)
        } else {
            bottomSheetBinding.ivAlbumImageLarge.setImageResource(R.drawable.ic_baseline_music_note_24)
            bottomSheetBinding.bottomSheetSmall.ivAlbumImageSmall.setImageResource(R.drawable.ic_baseline_music_note_24)
        }

        bottomSheetBinding.tvSingerLarge.text = music.artist
        bottomSheetBinding.tvTitleLarge.text = music.title

        bottomSheetBinding.bottomSheetSmall.tvSingerSmall.text = music.artist
        bottomSheetBinding.bottomSheetSmall.tvTitleSmall.text = music.title

        bottomSheetBinding.tvDurationEnd.text = SimpleDateFormat("mm:ss").format(music?.duration)

        bottomSheetBinding.seekBar.max = music?.duration ?: 0
    }

    fun onClick(view: View?) {
        if (currentMusic != null) {
            when (view?.id) {
                R.id.ivPlaySmall, R.id.ivPlayLarge -> {
                    if (mediaPlayer?.isPlaying == true) {
                        musicPause()
                    } else {
                        musicStart()
                    }
                }

                R.id.ivNextBottom -> {
                    musicNext()
                }

                R.id.ivPreviousBottom -> {
                    musicPrevious()
                }
            }
        }
    }

    fun musicStop() {
        mediaPlayer?.stop()
        playerJob?.cancel()
        mediaPlayer = null
        bottomSheetBinding.seekBar.progress = 0
        bottomSheetBinding.tvDurationStart.text = "00:00"
        bottomSheetBinding.bottomSheetSmall.ivPlaySmall.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        bottomSheetBinding.ivPlayLarge.setImageResource(R.drawable.ic_baseline_play_arrow_24)
    }

    fun musicPause() {
        mediaPlayer?.pause()
        isPlaying = false
        bottomSheetBinding.bottomSheetSmall.ivPlaySmall.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        bottomSheetBinding.ivPlayLarge.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        bottomSheetBinding.seekBar.progress = mediaPlayer?.currentPosition!!
    }

    fun musicStart() {
        mediaPlayer?.start()
        isPlaying = true
        bottomSheetBinding.bottomSheetSmall.ivPlaySmall.setImageResource(R.drawable.ic_baseline_pause_24)
        bottomSheetBinding.ivPlayLarge.setImageResource(R.drawable.ic_baseline_pause_24)

        //음악 재생 시간, seekBar ui 업데이트를 위한 Coroutines
        //ui를 바꾸는 작업은 runOnUiThread { } 에서 처리
        val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
        playerJob = backgroundScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                //현재 노래 재생 시간을 구하여 seekBar에 적용
                runOnUiThread {
                    var currentPosition = mediaPlayer?.currentPosition!!
                    bottomSheetBinding.seekBar.progress = currentPosition
                    bottomSheetBinding.tvDurationStart.text =
                        SimpleDateFormat("mm:ss").format(currentPosition)
                }

                try {
                    delay(500) //위의 runOnUiThread의 실행을 늦추기 위함
                } catch (e: Exception) {
                    Log.d("Log_debug", "delay error : ${e.printStackTrace()}")
                }
            }
            runOnUiThread {
                if (mediaPlayer!!.currentPosition >= (bottomSheetBinding.seekBar.max - 1000)) {
                    musicNext()
                }
            }
        }
    }

    fun musicNext(){

        if (playMusicList.isEmpty()){
            Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        musicStop()

        var nextMusic: Music? = null

        if (playMusicList.indexOf(currentMusic) != playMusicList.lastIndex){
            nextMusic = playMusicList[playMusicList.indexOf(currentMusic) + 1]
            mediaPlayer = MediaPlayer.create(this, nextMusic.getMusicUri())
            bottomSheetBinding.seekBar.max = nextMusic.duration ?: 0
        } else {
            nextMusic = playMusicList.first()
            mediaPlayer = MediaPlayer.create(this, nextMusic.getMusicUri())
            bottomSheetBinding.seekBar.max = nextMusic.duration ?: 0
        }

        if (nextMusic != null) {
            currentMusic = nextMusic
            bottomSheetUpdate(nextMusic)
        }

        if (isPlaying){
            musicStart()
        }
    }

    fun musicPrevious(){

        if (playMusicList.isEmpty()){
            Toast.makeText(this, "Playlist is empty", Toast.LENGTH_SHORT).show()
            return
        }

        when (mediaPlayer?.currentPosition) {
            in 0..5000 -> {
                musicStop()

                var previousMusic: Music? = null

                if (playMusicList.indexOf(currentMusic) != 0){
                    previousMusic = playMusicList[playMusicList.indexOf(currentMusic) - 1]
                    mediaPlayer = MediaPlayer.create(this, previousMusic.getMusicUri())
                    bottomSheetBinding.seekBar.max = previousMusic.duration ?: 0
                } else {
                    previousMusic = playMusicList.last()
                    mediaPlayer = MediaPlayer.create(this, previousMusic.getMusicUri())
                    bottomSheetBinding.seekBar.max = previousMusic.duration ?: 0
                }

                if (previousMusic != null) {
                    currentMusic = previousMusic
                    bottomSheetUpdate(previousMusic)
                }
            }

            else -> {
                musicStop()
                mediaPlayer = MediaPlayer.create(this, currentMusic?.getMusicUri())
            }
        }

        if (isPlaying) {
            musicStart()
        }
    }
}