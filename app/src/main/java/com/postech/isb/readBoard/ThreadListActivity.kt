package com.postech.isb.readBoard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.postech.isb.PostechIsb
import com.postech.isb.R
import com.postech.isb.boardList.ThreadAction
import com.postech.isb.compose.NoteEditorActivity
import com.postech.isb.databinding.ActivityThreadListBinding
import com.postech.isb.readThread.ReadThreadActivity
import kotlinx.coroutines.launch

class ThreadListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThreadListBinding
    private var lastReadIndex = -1
    private var longClickedThread: IsbThread? = null

    private val boardName: String by lazy {
        intent.getStringExtra("board") ?: "Unknown"
    }

    private val viewModel: ThreadListViewModel by viewModels {
        ThreadListViewModelFactory(
            (application as PostechIsb).boardRepository,
            boardName
        )
    }

    private val threadReaderResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lastReadIndex = result.data?.getIntExtra("idx", -1) ?: -1
        }
    }

    // New launcher for getting the result from our NoteEditorActivity
    private val noteEditorResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val title = result.data?.getStringExtra("result_title") ?: ""
            val content = result.data?.getStringExtra("result_content") ?: ""
            if (title.isNotBlank() || content.isNotBlank()) {
                viewModel.writeNewThread(title, content)
            }
        }
    }

    private val threadAdapter = ThreadListAdapter(
        onThreadClicked = { thread ->
            val intent = Intent(this, ReadThreadActivity::class.java).apply {
                putExtra("board", boardName)
                putExtra("key", thread.key)
            }
            threadReaderResultLauncher.launch(intent)
        },
        onThreadLongClicked = { thread, view ->
            longClickedThread = thread
            registerForContextMenu(view)
            openContextMenu(view)
            unregisterForContextMenu(view)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThreadListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupPagingControls()
        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleOnResume(lastReadIndex)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        binding.threadRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ThreadListActivity)
            adapter = threadAdapter
        }
    }

    private fun setupPagingControls() {
        binding.prevList.setOnClickListener {
            viewModel.loadPreviousPage()
        }
        binding.nextList.setOnClickListener {
            viewModel.loadNextPage()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    supportActionBar?.title = state.boardTitle
                    binding.progressBar.isVisible = state.isLoading
                    threadAdapter.submitList(state.threads)

                    binding.prevList.isEnabled = state.hasPreviousPage
                    binding.nextList.isEnabled = state.hasNextPage

                    state.error?.let {
                        Toast.makeText(this@ThreadListActivity, it, Toast.LENGTH_LONG).show()
                    }

                    state.singleEventMessage?.let { message ->
                        Toast.makeText(this@ThreadListActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearSingleEventMessage()
                    }

                    state.readersInfo?.let { readers ->
                        AlertDialog.Builder(this@ThreadListActivity)
                            .setTitle("Readers")
                            .setMessage(readers)
                            .setPositiveButton("OK", null)
                            .show()
                        viewModel.clearReadersInfo()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_boards, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.write_thread -> {
                // CORRECTED: Launch the new NoteEditorActivity to get a result
                val intent = Intent(this, NoteEditorActivity::class.java)
                noteEditorResultLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.thread_context_menu, menu)
        menu.setHeaderTitle("Thread Actions")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val thread = longClickedThread ?: return false

        return when (item.itemId) {
            R.id.context_delete -> {
                showDeleteConfirmationDialog(thread)
                true
            }
            R.id.context_inverse -> {
                viewModel.modifyThread(thread.key, ThreadAction.INVERSE)
                true
            }
            R.id.context_mark -> {
                viewModel.modifyThread(thread.key, ThreadAction.MARK)
                true
            }
            R.id.context_reader -> {
                viewModel.viewReaders(thread.key)
                true
            }
            R.id.context_edit -> {
                // TODO: Implement navigation to the note editor for editing a thread
                Toast.makeText(this, "Edit clicked for ${thread.title}", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog(thread: IsbThread) {
        AlertDialog.Builder(this)
            .setTitle("Delete Thread")
            .setMessage("Are you sure you want to delete this thread?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.modifyThread(thread.key, ThreadAction.DELETE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
