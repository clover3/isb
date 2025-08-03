package com.postech.isb.readThread

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.postech.isb.PostechIsb
import com.postech.isb.R
import com.postech.isb.databinding.ActivityReadThreadBinding
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class ReadThreadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadThreadBinding
    private val commentAdapter = CommentAdapter()

    private val boardName: String by lazy { intent.getStringExtra("board") ?: "" }
    private val threadKey: Int by lazy { intent.getIntExtra("key", 0) }

    private val viewModel: ReadThreadViewModel by viewModels {
        ReadThreadViewModelFactory(
            (application as PostechIsb).boardRepository,
            boardName,
            threadKey
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadThreadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        binding.threadComment.apply {
            layoutManager = LinearLayoutManager(this@ReadThreadActivity)
            adapter = commentAdapter
        }
    }

    private fun setupClickListeners() {
        binding.commentBtn.setOnClickListener {
            val commentText = binding.commentText.text.toString()
            if (commentText.isNotBlank()) {
                viewModel.postComment(commentText)
                binding.commentText.text?.clear()
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    state.threadContent?.let { content ->
                        binding.threadHead.text = "${content.title}\nby ${content.author}\n${content.date}"

                        val processedContent = processThreadContent(content.content)
                        binding.threadBody.text = processedContent
                        binding.threadBody.movementMethod = LinkMovementMethod.getInstance()
                        Linkify.addLinks(binding.threadBody, Linkify.WEB_URLS)

                        supportActionBar?.title = boardName

                        // CORRECTED: Access comments through the threadContent object
                        commentAdapter.submitList(content.comments)
                    }

                    // If threadContent is null, submit an empty list to clear the UI
                    if (state.threadContent == null) {
                        commentAdapter.submitList(emptyList())
                    }

                    state.error?.let {
                        Toast.makeText(this@ReadThreadActivity, it, Toast.LENGTH_LONG).show()
                    }

                    invalidateOptionsMenu()
                }
            }
        }
    }

    /**
     * This method migrates the custom text processing logic from your original ReadThread.java.
     */
    private fun processThreadContent(rawContent: CharSequence): CharSequence {
        val processedString = rawContent.toString().lines().joinToString("\n") { it.trim() }
        val spannable = SpannableStringBuilder(processedString)

        val highlightPattern = Pattern.compile("^!.*$", Pattern.MULTILINE)
        val highlightMatcher = highlightPattern.matcher(spannable)
        while (highlightMatcher.find()) {
            val start = highlightMatcher.start()
            val end = highlightMatcher.end()
            spannable.setSpan(ForegroundColorSpan(Color.BLACK), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(BackgroundColorSpan(Color.WHITE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.delete(start, start + 1)
        }

        val linkPattern = Pattern.compile("(\\s|^)[a-zA-Z0-9]/[a-zA-Z0-9]\\s\\d+(\\s|$)")
        val linkMatcher = linkPattern.matcher(spannable)
        while (linkMatcher.find()) {
            val start = linkMatcher.start()
            val end = linkMatcher.end()
            val linkText = linkMatcher.group(0)
            spannable.setSpan(BoardLinkSpan(linkText), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    // Custom ClickableSpan to handle board links
    inner class BoardLinkSpan(private val linkText: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            Toast.makeText(this@ReadThreadActivity, "Clicked on board link: $linkText", Toast.LENGTH_SHORT).show()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_thread_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val state = viewModel.uiState.value
        menu.findItem(R.id.prevThread)?.isVisible = state.hasPreviousThread
        menu.findItem(R.id.nextThread)?.isVisible = state.hasNextThread
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.prevThread -> {
                viewModel.loadPreviousThread()
                true
            }
            R.id.nextThread -> {
                viewModel.loadNextThread()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val resultIntent = Intent()
        resultIntent.putExtra("idx", viewModel.uiState.value.threadContent?.key)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
        return true
    }
}
