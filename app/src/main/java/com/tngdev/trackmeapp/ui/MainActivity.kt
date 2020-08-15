package com.tngdev.trackmeapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.tngdev.trackmeapp.AppPreferencesHelper
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.ui.history.HistoryFragment
import com.tngdev.trackmeapp.ui.recording.ForegroundOnlyLocationService
import com.tngdev.trackmeapp.ui.recording.RecordingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appPreferencesHelper: AppPreferencesHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HistoryFragment.newInstance())
                .commitNow()
        }

        checkGotoRecordingScreen(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkGotoRecordingScreen(intent)
    }

    private fun checkGotoRecordingScreen(intent: Intent?) {
        intent ?: return
        val fromNotification = intent.extras?.getBoolean(
            ForegroundOnlyLocationService.EXTRA_FROM_NOTIFICATION, false
        ) ?: false

//        if (fromNotification) {
            appPreferencesHelper.currentSessionId?.let {
                val activityIntent = Intent(this, RecordingActivity::class.java)
                startActivity(activityIntent)
            }
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}