package com.tngdev.trackmeapp.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tngdev.trackmeapp.R
import com.tngdev.trackmeapp.databinding.ActivityBaseBinding


open class BaseActivity : AppCompatActivity() {

    private var mHasActionbar : Boolean = true

    private var _binding : ActivityBaseBinding? = null
    private val binding get() = _binding!!

    protected open fun hostFragment() : Fragment? {
        // Implement in child class
        return null
    }

    protected fun addFragment(baseFragment: Fragment, containerId : Int? = null) {
        val fm = supportFragmentManager
        val tf = fm.beginTransaction()

        if (containerId == null) {
            tf.add(R.id.container, baseFragment, baseFragment.javaClass.name)
        }
        else {
            tf.add(containerId, baseFragment, baseFragment.javaClass.name)
        }
        tf.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hostFragment = hostFragment()
        if (hostFragment != null) {
            _binding = ActivityBaseBinding.inflate(layoutInflater)
            setContentView(binding.root)
            this.binding.toolbar.visibility = View.GONE
        }

        if (savedInstanceState == null && hostFragment != null) {
            addFragment(hostFragment)
        }
    }

    fun setHasActionbar(hasActionbar : Boolean) {
        mHasActionbar = hasActionbar

        if (mHasActionbar) {
            setSupportActionBar(binding.toolbar)
            binding.toolbar.visibility = View.VISIBLE
        }
        else {
            binding.toolbar.visibility = View.GONE
        }
    }
}