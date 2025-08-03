package com.postech.isb.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import com.postech.isb.PostechIsb
import com.postech.isb.R
import com.postech.isb.boardList.BoardListActivity
import com.postech.isb.databinding.FragmentLoginBinding
//import com.postech.isb.info.Info
//import com.postech.isb.query.Query
//import com.postech.isb.viewUser.ViewUser
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var credentialsManager: CredentialsManager
    private lateinit var noticeManager: NoticeManager

    private val viewModel: LoginViewModel by viewModels {
        val application = requireActivity().application as PostechIsb
        LoginViewModelFactory(
            application.isb,
            WorkManager.getInstance(requireContext()),
            noticeManager,
            application.settingsManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        credentialsManager = CredentialsManager(requireContext())
        noticeManager = NoticeManager(requireContext())

        setupClickListeners()
        observeUiState()
        loadSavedCredentials()
    }

    override fun onResume() {
        super.onResume()
        val credentials = credentialsManager.loadCredentials()
        viewModel.attemptAutoLogin(credentials)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_info -> {
//                startActivity(Intent(activity, Info::class.java))
                true
            }
            R.id.menu_user_list -> {
//                startActivity(Intent(activity, ViewUser::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClickListeners() {
        binding.loginBtn.setOnClickListener {
            val id = binding.loginId.text.toString()
            val pw = binding.loginPw.text.toString()
            credentialsManager.saveCredentials(id, pw, binding.rememberIdPw.isChecked)
            viewModel.login(id, pw)
        }

        binding.logoutBtn.setOnClickListener {
            viewModel.logout()
            credentialsManager.clearCredentials()
        }

        binding.goSurf.setOnClickListener {
            startActivity(Intent(activity, BoardListActivity::class.java))
        }

//        binding.query.setOnClickListener {
//            startActivity(Intent(activity, Query::class.java))
//        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.idPwInput.isVisible = !state.isLoggedIn
                    binding.logedIn.isVisible = state.isLoggedIn

                    if (state.isLoggedIn) {
                        binding.currentId.text = state.userId
                        binding.mailIcon.isVisible = state.hasNewMail
                    }

                    state.error?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }

                    if (state.showNoticeDialog) {
                        showNoticeDialog()
                        viewModel.onNoticeShown()
                    }
                }
            }
        }
    }

    private fun showNoticeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.notice_title)
            .setMessage(R.string.notice_content)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadSavedCredentials() {
        val credentials = credentialsManager.loadCredentials()
        if (credentials.rememberMe) {
            binding.loginId.setText(credentials.id)
            binding.loginPw.setText(credentials.pass)
            binding.rememberIdPw.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
