package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrekDao {

    @Query("SELECT * FROM treks ORDER BY startTimestamp DESC")
    fun getAllTreks(): Flow<List<TrekEntity>>

    @Query("SELECT * FROM treks WHERE id = :id LIMIT 1")
    suspend fun getTrekById(id: Long): TrekEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrek(trek: TrekEntity): Long

    @Delete
    suspend fun deleteTrek(trek: TrekEntity)

    @Query("DELETE FROM treks WHERE id = :id")
    suspend fun deleteTrekById(id: Long)

    @Query("SELECT COUNT(*) FROM treks")
    fun getTrekCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceMeters), 0.0) FROM treks")
    fun getTotalDistanceMeters(): Flow<Double>

    @Query("SELECT COALESCE(SUM(elevationGainMeters), 0.0) FROM treks")
    fun getTotalElevationGainMeters(): Flow<Double>
}
