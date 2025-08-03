package com.postech.isb.boardList
import com.postech.isb.readBoard.IsbThread
import com.postech.isb.readThread.Comment
import com.postech.isb.readThread.ThreadContent
import com.postech.isb.ui.compose.Note
import com.postech.isb.ui.compose.NoteDao
import com.postech.isb.util.IsbSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.postech.isb.util.ThreadList as OldThread // Correctly alias the single item class
import java.util.regex.Pattern

enum class ThreadAction {
    DELETE,
    INVERSE,
    MARK
}

data class ThreadListPage(
    val threads: List<IsbThread>,
    val firstIndex: Int,
    val lastIndex: Int,
    val totalThreads: Int
)
//
//interface BoardRepository {
//    // Board methods
//    fun getBoards(): Flow<List<Board>>
//    suspend fun refreshBoardsFromServer()
//    suspend fun setFavorite(boardName: String, isFavorite: Boolean)
//
//    // Note methods
//    fun getAllNotes(): Flow<List<Note>>
//    suspend fun saveNote(note: Note)
//    suspend fun deleteNote(noteId: Long)
//
//    suspend fun getThreadsByPage(boardName: String, page: Int): ThreadListPage
//    suspend fun getThreadContent(boardName: String, threadKey: Int): ThreadContent
//    suspend fun getThreadsByIndexRange(boardName: String, firstIndex: Int, lastIndex: Int): ThreadListPage
//    suspend fun getLastPageThreads(boardName: String): ThreadListPage
//    suspend fun postComment(boardName: String, threadKey: Int, comment: String)
//
//    suspend fun modifyThread(boardName: String, threadKey: Int, action: ThreadAction): Boolean
//    suspend fun getThreadReaders(boardName: String, threadKey: Int): String?
//
//    suspend fun checkBoardsForNewPosts(onlyFavorites: Boolean)
//
//    suspend fun writeToBoard(boardName: String, title: String, content: String): Boolean
//    suspend fun findLatestDiaryPostWithTitle(title: String): ThreadContent?
//
//    suspend fun hasNewPosts(boardName: String): Boolean
//}



class BoardRepository(
    private val boardDao: BoardDao,
    private val noteDao: NoteDao,
    private val isbSession: IsbSession
) {

    fun getBoards(): Flow<List<Board>> {
        // Get the Flow of database entities from the DAO
        return boardDao.getAllBoards()
            .map { boardEntities ->
                // For each list of entities emitted by the Flow,
                // map it to a list of our UI Board models.
                boardEntities.map { entity ->
                    Board(
                        entity.name,
                        entity.isFavorite,
                        entity.isMyBoard
                        // hasNewPost and hasNewComment will be updated
                        // from a different data source later if needed.
                    )
                }
            }
    }
    suspend fun refreshBoardsFromServer() {
        // Run the network call on a background thread.
        withContext(Dispatchers.IO) {
            // 1. Call the original IsbSession method.
            val originalBoardList = isbSession.boardList

            // 2. Translate the old Board objects into new BoardEntity objects.
            val boardEntities = originalBoardList.map { board ->
                BoardEntity(
                    name = board.name,
                    isFavorite = board.favorite,
                    isMyBoard = board.myBoard
                )
            }

            // 3. Save the new entities to the Room database.
            boardDao.insertAll(boardEntities)
        }
    }

    suspend fun setFavorite(boardName: String, isFavorite: Boolean) {
        // This is a database-only operation, so it remains the same.
        boardDao.setFavorite(boardName, isFavorite)
    }

    fun getAllNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes()
    }

    suspend fun saveNote(note: Note) {
        noteDao.insertOrUpdateNote(note)
    }

    suspend fun deleteNote(noteId: Long) {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun getThreadContent(boardName: String, threadKey: Int): ThreadContent {
        return withContext(Dispatchers.IO) {
            // 1. Call the original IsbSession method.
            //    The returned object is of type com.postech.isb.util.IsbThread
            val originalThread = isbSession.readThread(boardName, threadKey)

            ThreadContent(
                key = threadKey, // CORRECTED: Populate the key from the input parameter
                title = originalThread.title,
                author = originalThread.writer,
                date = originalThread.date,
                content = originalThread.content,
                comments = parseCommentsFromString(originalThread.comments),
            )
        }
    }

    private fun parseCommentsFromString(commentsString: String?): List<Comment> {
        if (commentsString.isNullOrBlank()) {
            return emptyList()
        }

        val comments = mutableListOf<Comment>()

        // This is the same regular expression used in your original ReadThread.java and IsbSession.java
        val pattern = Pattern.compile("^\\s*(\\w+)\\((\\d+,\\d+:\\d+)\\):\"(.*)\"\\r?$")

        // We split the raw string into individual lines to process them one by one
        commentsString.lines().forEach { line ->
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                // If a line matches the pattern, we extract the captured groups
                val author = matcher.group(1) ?: ""
                val date = matcher.group(2) ?: ""
                val content = matcher.group(3) ?: ""
                comments.add(Comment(author, date, content))
            }
        }
        return comments
    }


    private fun translateOldThreadList(originalList: ArrayList<OldThread>): ThreadListPage {
        if (originalList.isEmpty()) {
            return ThreadListPage(emptyList(), 0, 0, 0)
        }

        val threads = originalList.map { oldThread -> // oldThread is a ThreadList.java object
            IsbThread(
                key = oldThread.num,
                title = oldThread.title,
                author = oldThread.writer,
                date = oldThread.date,
                hasComment = oldThread.comment,
                isNotice = oldThread.notdel,
                isNew = oldThread.newt,
                isHighlighted = oldThread.highlight
            )
        }

        // Replicate the logic from ReadBoards.java to determine indices
        val firstIndex = originalList.last().num // The list is sorted descending by num
        val lastIndex = originalList.first().num

        return ThreadListPage(
            threads = threads,
            firstIndex = firstIndex,
            lastIndex = lastIndex,
            totalThreads = lastIndex // In the original code, the highest num was the total
        )
    }

    suspend fun getThreadsByIndexRange(boardName: String, firstIndex: Int, lastIndex: Int): ThreadListPage {
        return withContext(Dispatchers.IO) {
            val originalThreadList = isbSession.getThreadList(boardName, firstIndex, lastIndex)
            translateOldThreadList(originalThreadList)
        }
    }


    suspend fun getLastPageThreads(boardName: String): ThreadListPage {
        return withContext(Dispatchers.IO) {
            val originalThreadList = isbSession.getLastPageThreadList(boardName)
            translateOldThreadList(originalThreadList)
        }
    }

    suspend fun postComment(boardName: String, threadKey: Int, comment: String) {
        withContext(Dispatchers.IO) {
            isbSession.leaveComment(boardName, threadKey, comment)
        }
    }

    suspend fun modifyThread(boardName: String, threadKey: Int, action: ThreadAction): Boolean {
        return withContext(Dispatchers.IO) {
            val command = when (action) {
                ThreadAction.DELETE -> isbSession.THREAD_DELETE
                ThreadAction.INVERSE -> isbSession.THREAD_INVERSE
                ThreadAction.MARK -> isbSession.THREAD_MARK
            }
            isbSession.modifyThread(boardName, threadKey, command)
        }
    }

    suspend fun getThreadReaders(boardName: String, threadKey: Int): String? {
        return withContext(Dispatchers.IO) {
            isbSession.viewThreadReader(boardName, threadKey)
        }
    }
    suspend fun checkBoardsForNewPosts(onlyFavorites: Boolean) {
        withContext(Dispatchers.IO) {
            // If the request is not for favorites only, throw an exception to prevent
            // a long-running operation.
            if (!onlyFavorites) {
                throw UnsupportedOperationException("Checking all boards is not supported. Please check favorites only.")
            }

            // Get the current list of favorite boards from the database
            val boardsToCheck = boardDao.getFavoriteBoards()

            // Iterate through the boards and check for new posts
            for (board in boardsToCheck) {
                // Call the original IsbSession method
                val result = isbSession.searchNewPost(board.name)

                // Check the result and update the entity in the database if needed
                val hasNewPost = (result and 1) != 0
                val hasNewComment = (result and 2) != 0

                if (board.hasNewPost != hasNewPost || board.hasNewComment != hasNewComment) {
                    boardDao.updateNewPostStatus(board.name, hasNewPost, hasNewComment)
                }
            }
        }
    }

    suspend fun writeToBoard(boardName: String, title: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            isbSession.writeToBoard(boardName, title, content)
        }
    }

    suspend fun findLatestDiaryPostWithTitle(title: String): ThreadContent? {
        return withContext(Dispatchers.IO) {
            var page = getLastPageThreads("diary")
            var targetNum = -1

            while (true) {
                val foundThread = page.threads.find { it.title == title }
                if (foundThread != null) {
                    targetNum = foundThread.key
                    break
                }

                if (page.firstIndex <= 1) {
                    // We've reached the beginning of the diary and haven't found it
                    break
                }

                // Go to the previous page
                val threadsPerPage = 76 // From original ReadBoards.java
                val prevLastIndex = page.firstIndex - 1
                val prevFirstIndex = (prevLastIndex - threadsPerPage + 1).coerceAtLeast(1)
                page = getThreadsByIndexRange("diary", prevFirstIndex, prevLastIndex)
            }

            if (targetNum != -1) {
                // If we found the thread, fetch its full content
                return@withContext getThreadContent("diary", targetNum)
            } else {
                return@withContext null
            }
        }
    }
    suspend fun hasNewPosts(boardName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = isbSession.searchNewPost(boardName)
            // The original code checks if either the new post or new comment bit is set
            (result and 3) != 0
        }
    }

    suspend fun editPost(boardName: String, threadKey: Int, title: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            // This calls the original IsbSession method to edit a post.
            isbSession.EditToBoard(boardName, title, content, threadKey)
        }
    }


}
