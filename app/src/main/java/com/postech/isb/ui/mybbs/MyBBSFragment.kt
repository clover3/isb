package com.postech.isb.ui.mybbs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.postech.isb.PostechIsb
import com.postech.isb.databinding.FragmentMybbsBinding
import com.postech.isb.preference.SettingsActivity // CORRECTED: Use new SettingsActivity
import com.postech.isb.readBoard.ThreadListActivity // CORRECTED: Use new ThreadListActivity
import kotlinx.coroutines.launch

class MyBBSFragment : Fragment() {

    private var _binding: FragmentMybbsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyBBSViewModel by viewModels {
        MyBBSViewModelFactory((requireActivity().application as PostechIsb).isb)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMybbsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }

    private fun setupClickListeners() {
        binding.diary.setOnClickListener {
            // CORRECTED: Navigate to the new ThreadListActivity
            val intent = Intent(activity, ThreadListActivity::class.java).apply {
                putExtra("board", "diary")
            }
            startActivity(intent)
        }
        binding.mail.setOnClickListener {
            // CORRECTED: Navigate to the new ThreadListActivity
            val intent = Intent(activity, ThreadListActivity::class.java).apply {
                putExtra("board", "mail")
            }
            startActivity(intent)
        }
        binding.preference.setOnClickListener {
            // CORRECTED: Navigate to the new SettingsActivity
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoggedIn) {
                        binding.currentId.text = state.userId
                    } else {
                        binding.currentId.text = "MyBBS"
                    }

                    if (state.hasNewMail) {
                        binding.helloment.text = "New mail!"
                        binding.mailIcon.isVisible = true
                    } else {
                        binding.helloment.text = "Hello,"
                        binding.mailIcon.isVisible = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
