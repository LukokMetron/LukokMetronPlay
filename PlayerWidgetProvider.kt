package com.lukokmetron.player
import android.app.*
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
class PlayerWidgetProvider:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{update(c,m,it,"LukokMetron","Abra o player para escolher a pasta",false)}}
 override fun onReceive(c:Context,i:Intent){super.onReceive(c,i); val a=when(i.action){ACTION_PLAY_PAUSE->PlayerService.ACTION_TOGGLE;ACTION_NEXT->PlayerService.ACTION_NEXT;ACTION_PREV->PlayerService.ACTION_PREV;else->null}; if(a!=null)c.startService(Intent(c,PlayerService::class.java).setAction(a)) else if(i.action==ACTION_OPEN)c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
 companion object{
 const val ACTION_PLAY_PAUSE="com.lukokmetron.player.PLAY_PAUSE";const val ACTION_NEXT="com.lukokmetron.player.NEXT";const val ACTION_PREV="com.lukokmetron.player.PREV";const val ACTION_OPEN="com.lukokmetron.player.OPEN"
 fun refresh(c:Context,t:String,s:String,p:Boolean){val m=AppWidgetManager.getInstance(c);val comp=ComponentName(c,PlayerWidgetProvider::class.java);m.getAppWidgetIds(comp).forEach{update(c,m,it,t,s,p)}}
 private fun update(c:Context,m:AppWidgetManager,id:Int,t:String,s:String,p:Boolean){val v=RemoteViews(c.packageName,R.layout.player_widget);v.setTextViewText(R.id.widget_title,t);v.setTextViewText(R.id.widget_status,s);v.setTextViewText(R.id.widget_play,if(p)"⏸" else "▶");v.setOnClickPendingIntent(R.id.widget_play,pending(c,ACTION_PLAY_PAUSE));v.setOnClickPendingIntent(R.id.widget_next,pending(c,ACTION_NEXT));v.setOnClickPendingIntent(R.id.widget_prev,pending(c,ACTION_PREV));m.updateAppWidget(id,v)}
 private fun pending(c:Context,a:String)=PendingIntent.getBroadcast(c,a.hashCode(),Intent(c,PlayerWidgetProvider::class.java).setAction(a),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 }}
