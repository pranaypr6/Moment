package com.moment.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey val id: String,
    val relationshipId: String,
    val creatorId: String,
    val creatorName: String,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val note: String?,
    val wallpaperTarget: String,
    val isFavorite: Boolean,
    val status: String,
    val createdAt: Long
)

@Dao
interface MomentDao {
    @Query("SELECT * FROM moments WHERE relationshipId = :relationshipId ORDER BY createdAt DESC")
    fun getMomentsForRelationship(relationshipId: String): Flow<List<MomentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: MomentEntity)

    @Query("DELETE FROM moments WHERE id = :momentId")
    suspend fun deleteMoment(momentId: String)
}

@Database(entities = [MomentEntity::class], version = 2, exportSchema = false)
abstract class MomentDatabase : RoomDatabase() {
    abstract fun momentDao(): MomentDao
}
