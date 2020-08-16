package com.tngdev.trackmeapp.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tngdev.trackmeapp.databinding.FragmentHistoryBinding
import com.tngdev.trackmeapp.ui.recording.RecordingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    companion object {
        fun newInstance() = HistoryFragment()
    }

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewModel

    lateinit var adapter : HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.fab.setOnClickListener { view ->
            startActivity(Intent(requireActivity(), RecordingActivity::class.java))
        }

        viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
        setupUI()
    }

    private fun setupUI() {
        adapter = HistoryAdapter()
        binding.rvHistorySessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistorySessions.adapter = adapter
        // Subscribe the adapter to the ViewModel, so the items in the adapter are refreshed
        // when the list changes
        lifecycleScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            viewModel.historySessions.collectLatest { adapter.submitData(it) }
        }
    }
}