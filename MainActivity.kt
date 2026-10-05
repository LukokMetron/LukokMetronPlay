package com.lukokmetron.player

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val PICK_FOLDER=4001
    private val prefs by lazy { getSharedPreferences("native_player", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setup(); handleAction(intent?.action) }
    private fun setup(){
        web=WebView(this); setContentView(web)
        web.settings.javaScriptEnabled=true; web.settings.domStorageEnabled=true; web.settings.databaseEnabled=true; web.settings.mediaPlaybackRequiresUserGesture=false; web.settings.allowFileAccess=true; web.settings.allowContentAccess=true
        web.webViewClient=object:WebViewClient(){ override fun onPageFinished(v:WebView?,url:String?){ sendNativeSongs() } }
        web.webChromeClient=WebChromeClient(); web.addJavascriptInterface(Bridge(),"AndroidLukok")
        web.loadUrl("file:///android_asset/index.html")
    }
    override fun onNewIntent(i:Intent?){ super.onNewIntent(i); setIntent(i); handleAction(i?.action) }
    private fun handleAction(a:String?){ when(a){PlayerWidgetProvider.ACTION_PLAY_PAUSE->sendService(PlayerService.ACTION_TOGGLE);PlayerWidgetProvider.ACTION_NEXT->sendService(PlayerService.ACTION_NEXT);PlayerWidgetProvider.ACTION_PREV->sendService(PlayerService.ACTION_PREV)} }
    private fun sendService(action:String){ startService(Intent(this,PlayerService::class.java).setAction(action)) }
    private fun sendNativeSongs(){ val raw=prefs.getString("songs","[]")?:"[]"; web.post{web.evaluateJavascript("window.onNativeSongs && window.onNativeSongs(${JSONObject.quote(raw)});",null)} }
    private fun chooseFolder(){ startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),PICK_FOLDER) }
    override fun onActivityResult(req:Int,result:Int,data:Intent?){ super.onActivityResult(req,result,data); if(req==PICK_FOLDER && result==RESULT_OK && data?.data!=null){ val tree=data.data!!; contentResolver.takePersistableUriPermission(tree,Intent.FLAG_GRANT_READ_URI_PERMISSION); val list=scan(tree); val a=JSONArray(); list.forEach{a.put(JSONObject().put("id",it.id).put("title",it.title).put("uri",it.uri))}; prefs.edit().putString("songs",a.toString()).apply(); startService(Intent(this,PlayerService::class.java)); web.post{web.evaluateJavascript("window.onNativeSongs && window.onNativeSongs(${JSONObject.quote(a.toString())});",null)} } }
    private fun scan(tree:Uri):List<PlayerService.Song>{ val out=mutableListOf<PlayerService.Song>(); val doc=android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(tree,android.provider.DocumentsContract.getTreeDocumentId(tree)); contentResolver.query(doc,arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE),null,null,null)?.use{c-> val id=c.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID); val name=c.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME); val mime=c.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE); while(c.moveToNext()){val n=c.getString(name); val m=c.getString(mime); if(m.startsWith("audio/")||n.matches(Regex(".*\\.(mp3|m4a|aac|wav|flac|ogg|opus)$",RegexOption.IGNORE_CASE))){val uri=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(id)); out.add(PlayerService.Song(n,n.substringBeforeLast('.'),uri.toString()))}}}; return out.sortedBy{it.title.lowercase()} }
    inner class Bridge { @JavascriptInterface fun pickFolder(){runOnUiThread{chooseFolder()}} @JavascriptInterface fun isNativePlayer()=true }
}
