package com.tngdev.trackmeapp.ui.recording

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.tngdev.trackmeapp.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecordingActivity : BaseActivity() {

    private lateinit var mFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasActionbar(false)
    }

    override fun hostFragment(): Fragment? {
        mFragment = RecordingFragment.newInstance()
        return mFragment
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        mFragment.onActivityResult(requestCode, resultCode, data)
    }

}