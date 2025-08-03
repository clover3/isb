package com.postech.isb.boardList

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards")
    fun getAllBoards(): Flow<List<BoardEntity>>

    // ADDED: A method to get a non-Flow list for one-time checks
    @Query("SELECT * FROM boards")
    suspend fun getAllBoardsOnce(): List<BoardEntity>

    // ADDED: A method to get only favorite boards
    @Query("SELECT * FROM boards WHERE isFavorite = 1")
    suspend fun getFavoriteBoards(): List<BoardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(boards: List<BoardEntity>)

    @Query("UPDATE boards SET isFavorite = :isFavorite WHERE name = :boardName")
    suspend fun setFavorite(boardName: String, isFavorite: Boolean)

    // ADDED: A method to update the new post status for a single board
    @Query("UPDATE boards SET hasNewPost = :hasNewPost, hasNewComment = :hasNewComment WHERE name = :boardName")
    suspend fun updateNewPostStatus(boardName: String, hasNewPost: Boolean, hasNewComment: Boolean)
}
