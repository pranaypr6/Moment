package com.moment.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val note: String?,
    val wallpaperTarget: String,
    val status: String,
    val createdAt: Long
)

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments ORDER BY createdAt DESC")
    fun getAllMoments(): Flow<List<MomentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: MomentEntity)

    @Query("DELETE FROM moments WHERE id = :momentId")
    suspend fun deleteMoment(momentId: String)
}

@Database(entities = [MomentEntity::class], version = 1, exportSchema = false)
abstract class MomentDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
}
