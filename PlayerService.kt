package com.lukokmetron.player

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject

class PlayerService : Service() {
    private var player: MediaPlayer? = null
    private var session: MediaSession? = null
    private var currentIndex = -1
    private var songs: MutableList<Song> = mutableListOf()
    private val prefs by lazy { getSharedPreferences("native_player", MODE_PRIVATE) }

    data class Song(val id:String, val title:String, val uri:String)

    override fun onCreate() {
        super.onCreate()
        loadSongs()
        createNotificationChannel()
        session = MediaSession(this, "LukokMetron").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { playPause(true) }
                override fun onPause() { playPause(false) }
                override fun onSkipToNext() { next() }
                override fun onSkipToPrevious() { previous() }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playPause(true)
            ACTION_PAUSE -> playPause(false)
            ACTION_TOGGLE -> if (player?.isPlaying == true) playPause(false) else playPause(true)
            ACTION_NEXT -> next()
            ACTION_PREV -> previous()
            ACTION_PLAY_URI -> {
                val uri = intent.getStringExtra(EXTRA_URI) ?: return START_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "LukokMetron"
                val id = intent.getStringExtra(EXTRA_ID) ?: uri
                val idx = songs.indexOfFirst { it.id == id }
                if (idx >= 0) currentIndex = idx else { songs.add(Song(id,title,uri)); currentIndex=songs.lastIndex }
                startCurrent(true)
            }
        }
        return START_STICKY
    }

    private fun loadSongs() {
        val a = JSONArray(prefs.getString("songs", "[]"))
        songs.clear()
        for (i in 0 until a.length()) {
            val o=a.getJSONObject(i); songs.add(Song(o.getString("id"),o.getString("title"),o.getString("uri")))
        }
        currentIndex = prefs.getInt("index", -1).coerceIn(-1, songs.lastIndex)
    }

    private fun saveSongs() {
        val a=JSONArray(); songs.forEach { a.put(JSONObject().put("id",it.id).put("title",it.title).put("uri",it.uri)) }
        prefs.edit().putString("songs",a.toString()).apply()
    }

    fun replaceSongs(list: List<Song>) { songs.clear(); songs.addAll(list); saveSongs(); currentIndex=-1; stopPlayer(); updateWidget("LukokMetron","Pasta pronta",false) }

    private fun startCurrent(auto:Boolean) {
        if (currentIndex !in songs.indices) return
        val s=songs[currentIndex]
        try {
            player?.release()
            player=MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                setDataSource(this@PlayerService, Uri.parse(s.uri))
                setOnPreparedListener { mp -> if(auto) mp.start(); updateState(); updateNotification(); updateWidget(s.title, if(auto) "Reproduzindo" else "Pausada", auto) }
                setOnCompletionListener { next() }
                prepareAsync()
            }
            prefs.edit().putInt("index",currentIndex).apply()
        } catch(e:Exception) { updateWidget(s.title,"Erro ao reproduzir",false) }
    }

    private fun playPause(play:Boolean) {
        if (player==null) { if (currentIndex<0 && songs.isNotEmpty()) currentIndex=0; startCurrent(play); return }
        if (play) player?.start() else player?.pause()
        updateState(); updateNotification(); currentTitle()?.let { updateWidget(it, if(play) "Reproduzindo" else "Pausada", play) }
    }
    private fun next() { if(songs.isEmpty()) return; currentIndex=(if(currentIndex<0) 0 else (currentIndex+1)%songs.size); startCurrent(true) }
    private fun previous() { if(songs.isEmpty()) return; currentIndex=(if(currentIndex<=0) songs.lastIndex else currentIndex-1); startCurrent(true) }
    private fun currentTitle()=if(currentIndex in songs.indices) songs[currentIndex].title else "LukokMetron"
    private fun stopPlayer(){ player?.release(); player=null; stopForeground(STOP_FOREGROUND_REMOVE) }

    private fun updateState(){ session?.setPlaybackState(android.media.session.PlaybackState.Builder().setActions(android.media.session.PlaybackState.ACTION_PLAY or android.media.session.PlaybackState.ACTION_PAUSE or android.media.session.PlaybackState.ACTION_SKIP_TO_NEXT or android.media.session.PlaybackState.ACTION_SKIP_TO_PREVIOUS).setState(if(player?.isPlaying==true) android.media.session.PlaybackState.STATE_PLAYING else android.media.session.PlaybackState.STATE_PAUSED, player?.currentPosition?.toLong()?:0L, 1f).build()) }

    private fun updateNotification(){
        val title=currentTitle(); val playing=player?.isPlaying==true
        val open=PendingIntent.getActivity(this,10,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val prev=PendingIntent.getService(this,11,Intent(this,PlayerService::class.java).setAction(ACTION_PREV),PendingIntent.FLAG_IMMUTABLE)
        val toggle=PendingIntent.getService(this,12,Intent(this,PlayerService::class.java).setAction(ACTION_TOGGLE),PendingIntent.FLAG_IMMUTABLE)
        val next=PendingIntent.getService(this,13,Intent(this,PlayerService::class.java).setAction(ACTION_NEXT),PendingIntent.FLAG_IMMUTABLE)
        val n=Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(if(playing)"Reproduzindo" else "Pausada").setContentIntent(open).setOngoing(playing).addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,android.R.drawable.ic_media_previous),"Anterior",prev).build()).addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,if(playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play),if(playing)"Pausar" else "Reproduzir",toggle).build()).addAction(Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this,android.R.drawable.ic_media_next),"Próxima",next).build()).build()
        startForeground(NOTIF_ID,n)
    }
    private fun updateWidget(title:String,status:String,playing:Boolean)=PlayerWidgetProvider.refresh(this,title,status,playing)
    private fun createNotificationChannel(){ if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"LukokMetron",NotificationManager.IMPORTANCE_LOW)) }
    override fun onBind(intent:Intent?)=null
    override fun onDestroy(){ session?.release(); player?.release(); super.onDestroy() }

    companion object { const val CHANNEL="lukokmetron_playback"; const val NOTIF_ID=1701; const val ACTION_PLAY="PLAY"; const val ACTION_PAUSE="PAUSE"; const val ACTION_TOGGLE="TOGGLE"; const val ACTION_NEXT="NEXT"; const val ACTION_PREV="PREV"; const val ACTION_PLAY_URI="PLAY_URI"; const val EXTRA_ID="id"; const val EXTRA_TITLE="title"; const val EXTRA_URI="uri" }
}
