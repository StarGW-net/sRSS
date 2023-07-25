package net.frju.flym.usage

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.fred.feedex.R
import net.frju.flym.App.Companion.context
import net.frju.flym.utils.setupNoActionBarTheme
import java.text.SimpleDateFormat
import java.util.*


class UsageActivity  : AppCompatActivity() {

    companion object {

        private const val LOG = "STEVE_USAGE"
        private lateinit var myContext :Context

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupNoActionBarTheme()

        super.onCreate(savedInstanceState)

        myContext = this

        setContentView(R.layout.activity_usage)

        val networkStatsManager :NetworkStatsManager = myContext.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val manager = context.packageManager
        val info = manager.getApplicationInfo("net.frju.flym", 0)
        val uid = info.uid

        Log.w(LOG,"UID = " + uid)

        val time :Long = Date().getTime() - (Date().getTimezoneOffset() * 60 * 1000)
        Log.w(LOG,"TIME = " + time)
        Log.w(LOG,"OFFSET = " + Date(). getTimezoneOffset())

        val nwStatsWifi: NetworkStats = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, null, (time - (3* 86400000)), time, uid)

        var receivedWifi :Long = 0
        var sentWifi :Long = 0

        val items: ArrayList<String> = ArrayList<String>()

        val bucketWifi = NetworkStats.Bucket()
        while (nwStatsWifi.hasNextBucket()) {
            nwStatsWifi.getNextBucket(bucketWifi)

            val start = bucketWifi.getStartTimeStamp()
            val end = bucketWifi.getEndTimeStamp()

            val format1 = SimpleDateFormat("HH:mm:ss E d MMM YYYY")
            val format2 = SimpleDateFormat("HH:mm:ss")

            val line = format2.format(start) + " - " + format1.format(end) + "\nTx = " + bucketWifi.txBytes + " , Rx = " +  bucketWifi.rxBytes
            // Log.w(LOG, line )
            items.add(line)

            receivedWifi = receivedWifi + bucketWifi.rxBytes
            sentWifi = sentWifi + bucketWifi.txBytes
        }

        Log.w(LOG,"Received WiFi = " + receivedWifi)

        var tt1: TextView = this.findViewById(R.id.usageReceived)
        tt1.setText("Bytes Received = " + receivedWifi)

        var tt2: TextView = this.findViewById(R.id.usageSent)
        tt2.setText("Bytes Sent = " + sentWifi)

        val itemsAdapter = ArrayAdapter(this, R.layout.activity_usage_row1, items)
        // adapter.updateFull();
        // notify?
        itemsAdapter.notifyDataSetChanged()
        var myListView = findViewById<View>(R.id.usageList) as ListView
        myListView.setAdapter(itemsAdapter)

    }

}