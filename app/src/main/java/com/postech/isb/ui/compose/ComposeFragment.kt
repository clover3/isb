package com.postech.isb.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.postech.isb.PostechIsb
import com.postech.isb.databinding.FragmentComposeBinding
import kotlinx.coroutines.launch

class ComposeFragment : Fragment() {

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ComposeViewModel by viewModels {
        ComposeViewModelFactory((requireActivity().application as PostechIsb).boardRepository)
    }

    private lateinit var noteAdapter: NoteListAdapter
    private lateinit var emptyTextView: TextView // A property to hold the empty view

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        emptyTextView = view.findViewById(android.R.id.empty)
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        setupBackButtonHandler()
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteListAdapter(
            onNoteClicked = { note ->
                viewModel.enterEditMode(note)
            },
            onNoteLongClicked = { note ->
                showDeleteConfirmationDialog(note)
            }
        )
        binding.notesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddNote.setOnClickListener {
            viewModel.enterEditMode() // No note passed means it's a new one
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    noteAdapter.submitList(state.notes)
                    emptyTextView.isVisible = state.notes.isEmpty() && state.mode == ComposeScreenMode.LIST

                    binding.notesRecyclerView.isVisible = state.mode == ComposeScreenMode.LIST
                    binding.fabAddNote.isVisible = state.mode == ComposeScreenMode.LIST
                    binding.composeNew.isVisible = state.mode == ComposeScreenMode.EDITING

                    if (state.mode == ComposeScreenMode.EDITING) {
                        binding.composeNewTitle.setText(state.currentlyEditingNote?.title ?: "")
                        binding.composeNewContent.setText(state.currentlyEditingNote?.content ?: "")
                    }
                }
            }
        }
    }

    private fun setupBackButtonHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (viewModel.uiState.value.mode == ComposeScreenMode.EDITING) {
                val title = binding.composeNewTitle.text.toString()
                val content = binding.composeNewContent.text.toString()
                if (title.isNotBlank() || content.isNotBlank()) {
                    viewModel.saveNote(title, content)
                } else {
                    viewModel.exitEditMode()
                }
            } else {
                isEnabled = false
                requireActivity().onBackPressed()
            }
        }
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(note.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
