package com.miraj.nexa

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Calendar
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var root: LinearLayout
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private var tts: TextToSpeech? = null
    private val prefs by lazy { getSharedPreferences("nexa", MODE_PRIVATE) }
    private val bg = Color.rgb(12,17,29)
    private val panel = Color.rgb(24,32,49)
    private val accent = Color.rgb(89,166,255)
    private val white = Color.rgb(241,245,249)
    private val muted = Color.rgb(164,178,198)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) { requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 902) }
        showHome()
    }
    private fun base(titleText: String): LinearLayout {
        root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(12)); setBackgroundColor(bg) }
        val title = TextView(this).apply { text=titleText; textSize=23f; setTextColor(accent); setPadding(dp(4),dp(8),dp(4),dp(14)) }
        root.addView(title)
        val nav=LinearLayout(this).apply { gravity=Gravity.CENTER; orientation=LinearLayout.HORIZONTAL }
        nav.addView(btn("Chat"){showHome()}, LinearLayout.LayoutParams(0,dp(44),1f))
        nav.addView(btn("Reminders"){showReminders()}, LinearLayout.LayoutParams(0,dp(44),1f))
        nav.addView(btn("Phone tools"){showTools()}, LinearLayout.LayoutParams(0,dp(44),1f))
        root.addView(nav)
        return root
    }
    private fun showHome() {
        val r=base("NEXA AI\nAdvanced Assistant")
        scroll=ScrollView(this); messages=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(4),0,dp(8)) }
        scroll.addView(messages); r.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val bar=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        input=EditText(this).apply { hint="Nexa-কে কিছু জিজ্ঞেস করো..."; setHintTextColor(muted); setTextColor(white); textSize=15f; maxLines=3; setPadding(dp(12),dp(8),dp(12),dp(8)); setBackgroundColor(panel) }
        bar.addView(input,LinearLayout.LayoutParams(0,dp(52),1f))
        bar.addView(btn("🎙"){voice()},LinearLayout.LayoutParams(dp(56),dp(52)))
        bar.addView(btn("➤"){send()},LinearLayout.LayoutParams(dp(56),dp(52)))
        r.addView(bar); setContentView(r)
        addMessage("Nexa","আসসালামু আলাইকুম! আমি Nexa। এখন Reminders থেকে নোটিফিকেশন সেট করতে পারবে। AI উত্তর পেতে নিরাপদ AI সার্ভার সংযোগও লাগবে।",false)
    }
    private fun showReminders() {
        val r=base("Reminders & Notifications")
        val info=TextView(this).apply { text="বার্তা ও সময় সেট করো। Nexa নির্ধারিত সময়ে নোটিফিকেশন দেবে।"; setTextColor(white); textSize=16f }
        r.addView(info)
        val message=EditText(this).apply { hint="যেমন: Biology class শুরু করো"; setTextColor(white); setHintTextColor(muted); setSingleLine(true); setBackgroundColor(panel) }
        r.addView(message,LinearLayout.LayoutParams(-1,dp(54)))
        val dateTime=Button(this).apply { text="সময় নির্বাচন করো" }
        r.addView(dateTime,LinearLayout.LayoutParams(-1,dp(48)))
        var chosen: Calendar?=null
        dateTime.setOnClickListener {
            val now=Calendar.getInstance()
            TimePickerDialog(this,{_,hour,minute->
                val c=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,hour); set(Calendar.MINUTE,minute); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0); if(timeInMillis<=System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }
                chosen=c; dateTime.text=String.format(Locale.getDefault(),"প্রতিদিন নয় — আজ/পরের দিন %02d:%02d",hour,minute)
            },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show()
        }
        r.addView(btn("＋ Save reminder"){ 
            val msg=message.text.toString().trim(); val c=chosen
            if(msg.isEmpty()||c==null){Toast.makeText(this,"বার্তা ও সময় দুটোই দাও",Toast.LENGTH_SHORT).show()}
            else { scheduleReminder(msg,c.timeInMillis); message.setText(""); Toast.makeText(this,"Reminder save হয়েছে",Toast.LENGTH_LONG).show(); showReminders() }
        },LinearLayout.LayoutParams(-1,dp(50)))
        r.addView(btn("Saved reminders"){showSavedReminders()},LinearLayout.LayoutParams(-1,dp(48)))
        val note=TextView(this).apply { text="নোট: ফোনের notification permission চালু রাখবে। Battery saver কিছু ফোনে notification দেরি করাতে পারে।"; setTextColor(muted); textSize=13f }
        r.addView(note); setContentView(r)
    }
    private fun scheduleReminder(msg:String, time:Long) {
        val id=(System.currentTimeMillis()%Int.MAX_VALUE).toInt()
        val json=JSONObject().put("id",id).put("msg",msg).put("time",time)
        val all=prefs.getString("reminders","[]") ?: "[]"
        val arr=org.json.JSONArray(all); arr.put(json); prefs.edit().putString("reminders",arr.toString()).apply()
        val intent=Intent(this,ReminderReceiver::class.java).putExtra("msg",msg).putExtra("id",id)
        val pi=PendingIntent.getBroadcast(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am=getSystemService(ALARM_SERVICE) as AlarmManager
        if(Build.VERSION.SDK_INT>=23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi) else am.set(AlarmManager.RTC_WAKEUP,time,pi)
    }
    private fun showSavedReminders() {
        val r=base("Saved reminders")
        val arr=org.json.JSONArray(prefs.getString("reminders","[]") ?: "[]")
        if(arr.length()==0) r.addView(label("এখনো কোনো reminder নেই।"))
        for(i in 0 until arr.length()) {
            val obj=arr.optJSONObject(i) ?: continue
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
            val txt=TextView(this).apply { text=obj.optString("msg")+"\n"+java.text.SimpleDateFormat("dd MMM, HH:mm",Locale.getDefault()).format(java.util.Date(obj.optLong("time"))); setTextColor(white); textSize=15f }
            row.addView(txt,LinearLayout.LayoutParams(0,-2,1f))
            row.addView(btn("Delete"){ cancelReminder(obj.optInt("id")); showSavedReminders() })
            r.addView(row)
        }
        setContentView(r)
    }
    private fun cancelReminder(id:Int) {
        val all=org.json.JSONArray(prefs.getString("reminders","[]") ?: "[]"); val next=org.json.JSONArray()
        for(i in 0 until all.length()){val o=all.optJSONObject(i) ?: continue; if(o.optInt("id")!=id) next.put(o)}
        prefs.edit().putString("reminders",next.toString()).apply()
        val pi=PendingIntent.getBroadcast(this,id,Intent(this,ReminderReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
    private fun showTools() {
        val r=base("Phone tools")
        r.addView(label("অনুমতি দিয়ে নিচের কাজগুলো চেষ্টা করতে পারো।"))
        r.addView(btn("Open browser / search"){startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.google.com")))},LinearLayout.LayoutParams(-1,dp(50)))
        r.addView(btn("Open phone settings"){startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))},LinearLayout.LayoutParams(-1,dp(50)))
        r.addView(btn("Set an alarm"){try{startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM).putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"Nexa reminder").putExtra(android.provider.AlarmClock.EXTRA_HOUR,7).putExtra(android.provider.AlarmClock.EXTRA_MINUTES,0))}catch(_:Exception){Toast.makeText(this,"Clock app পাওয়া যায়নি",Toast.LENGTH_SHORT).show()}},LinearLayout.LayoutParams(-1,dp(50)))
        r.addView(btn("AI connection info"){AlertDialog.Builder(this).setTitle("AI connection").setMessage("AI উত্তর চালু করতে একটি secure backend/API endpoint configure করতে হবে। API secret সরাসরি APK-তে রাখা নিরাপদ নয়। এই build-এ online AI এখনো সক্রিয় নয়।").setPositiveButton("OK",null).show()},LinearLayout.LayoutParams(-1,dp(50)))
        setContentView(r)
    }
    private fun send() {
        val q=input.text.toString().trim(); if(q.isEmpty()) return
        input.setText(""); addMessage("তুমি",q,true)
        val low=q.lowercase(Locale.ROOT)
        when {
            low.contains("open youtube") || low.contains("ইউটিউব খোলো") -> { startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://youtube.com"))); addMessage("Nexa","YouTube খুলছি।",false) }
            low.contains("open settings") || low.contains("সেটিংস খোলো") -> { startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)); addMessage("Nexa","Phone settings খুলছি।",false) }
            low.contains("alarm") || low.contains("অ্যালার্ম") -> { try{startActivity(Intent(android.provider.AlarmClock.ACTION_SET_ALARM))}catch(_:Exception){}; addMessage("Nexa","Clock app-এ alarm সেট করো।",false) }
            low.contains("কেমন আছ") || low.contains("how are you") -> addMessage("Nexa","আমি ভালো আছি! 😊 তুমি কেমন আছো? মনে রেখো, এই build-এ এখনো online AI যুক্ত হয়নি।",false)
            low.contains("নাম") || low.contains("name") -> addMessage("Nexa","আমার নাম Nexa।",false)
            else -> addMessage("Nexa","তোমার কথা বুঝেছি: “$q”। এই build-এ voice input, কিছু phone actions ও reminder আছে; বুদ্ধিমান AI উত্তর চালু করতে secure AI server সংযোগ এখনও লাগবে।",false)
        }
    }
    private fun addMessage(who:String, textMsg:String, user:Boolean) {
        val bubble=TextView(this).apply { text="$who\n$textMsg"; textSize=15f; setTextColor(white); setPadding(dp(13),dp(11),dp(13),dp(11)); setBackgroundColor(if(user) Color.rgb(31,75,120) else panel) }
        val row=LinearLayout(this).apply { gravity=if(user) Gravity.END else Gravity.START; setPadding(0,dp(5),0,dp(5)); addView(bubble,LinearLayout.LayoutParams((resources.displayMetrics.widthPixels*.82f).toInt(),-2)) }
        messages.addView(row); scroll.post{scroll.fullScroll(View.FOCUS_DOWN)}
        if(!user) messages.addView(TextView(this).apply { text="▶ উত্তর শুনুন"; setTextColor(muted); setPadding(dp(5),dp(4),0,dp(8)); setOnClickListener{tts?.speak(textMsg,TextToSpeech.QUEUE_FLUSH,null,"nexa")} })
    }
    private fun voice() {
        val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE,"bn-BD"); putExtra(RecognizerIntent.EXTRA_PROMPT,"Nexa-কে বলো...") }
        try{startActivityForResult(i,701)}catch(_:Exception){Toast.makeText(this,"Speech recognition পাওয়া যাচ্ছে না",Toast.LENGTH_LONG).show()}
    }
    @Deprecated("Deprecated by Android API")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data); if(requestCode==701&&resultCode==RESULT_OK){input.setText(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: "");send()}}
    private fun createNotificationChannel(){if(Build.VERSION.SDK_INT>=26){val c=NotificationChannel("nexa_reminders","Nexa reminders",NotificationManager.IMPORTANCE_HIGH).apply{description="Scheduled reminders from Nexa"};(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c)}}
    private fun btn(text:String, action:()->Unit)=Button(this).apply{this.text=text;setOnClickListener{action()}}
    private fun label(text:String)=TextView(this).apply{this.text=text;setTextColor(white);textSize=15f;setPadding(dp(6),dp(12),dp(6),dp(12))}
    override fun onInit(status:Int){if(status==TextToSpeech.SUCCESS) tts?.language=Locale("bn","BD")}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
}
