package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vpn_logs")
data class VpnLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // CONNECT, DISCONNECT, AI_OPTIMIZE, SECURITY_DIAGNOSTIC
    val serverName: String,
    val protocolName: String,
    val pingMs: Int,
    val notes: String
)

@Dao
interface VpnLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VpnLogEntity)

    @Query("SELECT * FROM vpn_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<VpnLogEntity>>

    @Query("DELETE FROM vpn_logs")
    suspend fun clearLogs()
}

@Database(entities = [VpnLogEntity::class], version = 1, exportSchema = false)
abstract class NexusVpnDatabase : RoomDatabase() {
    abstract fun vpnLogDao(): VpnLogDao
}
