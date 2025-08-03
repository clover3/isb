package com.postech.isb.compose

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.postech.isb.PostechIsb
import com.postech.isb.databinding.ActivityNoteEditorBinding
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding

    private val viewModel: NoteEditorViewModel by viewModels {
        NoteEditorViewModelFactory((application as PostechIsb).boardRepository)
    }

    // Check intent extras to see if we are in edit mode
    private val boardName: String? by lazy { intent.getStringExtra("board_name") }
    private val threadKey: Int by lazy { intent.getIntExtra("thread_key", -1) }
    private val isEditMode: Boolean by lazy { threadKey != -1 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill content if we are editing an existing post
        val initialTitle = intent.getStringExtra("initial_title")
        val initialContent = intent.getStringExtra("initial_content")
        binding.titleEditText.setText(initialTitle)
        binding.contentEditText.setText(initialContent)

        binding.saveButton.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val content = binding.contentEditText.text.toString()

            // Return the new title and content as the result
            val resultIntent = Intent().apply {
                putExtra("result_title", title)
                putExtra("result_content", content)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val content = binding.contentEditText.text.toString()

            if (isEditMode && boardName != null) {
                // If editing, save the changes
                viewModel.saveChanges(boardName!!, threadKey, title, content)
            } else {
                // If creating a new post, return the result
                val resultIntent = Intent().apply {
                    putExtra("result_title", title)
                    putExtra("result_content", content)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    state.initialContent?.let {
                        binding.titleEditText.setText(it.title)
                        binding.contentEditText.setText(it.content)
                    }

                    state.error?.let {
                        Toast.makeText(this@NoteEditorActivity, it, Toast.LENGTH_SHORT).show()
                    }

                    if (state.isSaveComplete) {
                        Toast.makeText(this@NoteEditorActivity, "Edit successful!", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
        }
    }
}
