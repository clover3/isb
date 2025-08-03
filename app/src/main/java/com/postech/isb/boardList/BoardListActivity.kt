package com.postech.isb.boardList

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.postech.isb.PostechIsb
import com.postech.isb.R
import com.postech.isb.databinding.BoardListBinding
import com.postech.isb.readBoard.ThreadListActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BoardListActivity : AppCompatActivity() {

    private lateinit var binding: BoardListBinding
    private var isPickMode: Boolean = false

    private val viewModel: BoardListViewModel by viewModels {
        BoardListViewModelFactory((application as PostechIsb).boardRepository)
    }

    private val adapter = BoardListAdapter(
        onFavoriteClicked = { board ->
            viewModel.onFavoriteClicked(board)
        },
        onBoardClicked = { board ->
            if (isPickMode) {
                // If we are in "pick" mode, set the result and finish.
                val resultIntent = Intent()
                resultIntent.data = Uri.parse("content://com.postech.provider.Isb/boards/${board.name}")
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                // Otherwise, navigate to the thread list screen.
                val intent = Intent(this, ThreadListActivity::class.java).apply {
                    putExtra("board", board.name)
                }
                startActivity(intent)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if the activity was launched with the ACTION_PICK intent.
        if (Intent.ACTION_PICK == intent.action) {
            isPickMode = true
            title = "Select a Board" // Update the title for clarity
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Listen for text changes in the search bar and notify the ViewModel
        binding.searchBoard.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Collect UI state from the ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    adapter.submitList(uiState.boards)
                    binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                    uiState.error?.let { errorMsg ->
                        Toast.makeText(this@BoardListActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }

                    invalidateOptionsMenu()
                }
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onRefresh()
        }

        lifecycleScope.launch {
            viewModel.uiState.map { it.isLoading }.distinctUntilChanged().collect { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkForNewPosts()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.board_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val filter = viewModel.uiState.value.activeFilter

        val favoriteItem = menu.findItem(R.id.favorite)
        if (filter == BoardFilter.FAVORITES_ONLY) {
            favoriteItem?.icon = ContextCompat.getDrawable(this, R.drawable.list_favorite_only_36)
        } else {
            favoriteItem?.icon = ContextCompat.getDrawable(this, R.drawable.list_all_36)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                viewModel.onRefresh()
                true
            }
            R.id.favorite -> {
                viewModel.setFilter(BoardFilter.FAVORITES_ONLY)
                true
            }
            R.id.search_new -> {
                // CORRECTED: This now triggers the background check for new posts.
                Toast.makeText(this, "Checking for new posts...", Toast.LENGTH_SHORT).show()
                viewModel.checkForNewPosts()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
