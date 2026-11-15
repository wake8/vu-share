package com.vunnam.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC")
    fun getAllTransfersFlow(): Flow<List<TransferRecordEntity>>

    @Query("SELECT * FROM transfer_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransfersFlow(limit: Int = 5): Flow<List<TransferRecordEntity>>

    @Query("SELECT * FROM transfer_history WHERE id = :id")
    suspend fun getTransferById(id: String): TransferRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(record: TransferRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfers(records: List<TransferRecordEntity>)

    @Update
    suspend fun updateTransfer(record: TransferRecordEntity)

    @Query("DELETE FROM transfer_history WHERE id = :id")
    suspend fun deleteTransferById(id: String)

    @Query("DELETE FROM transfer_history")
    suspend fun clearAllHistory()

    @Query("SELECT COUNT(*) FROM transfer_history")
    suspend fun getCount(): Int

    @Query("SELECT SUM(fileSize) FROM transfer_history WHERE status = 'COMPLETED'")
    suspend fun getTotalTransferredBytes(): Long?
}
