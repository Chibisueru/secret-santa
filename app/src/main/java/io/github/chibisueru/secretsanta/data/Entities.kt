package io.github.chibisueru.secretsanta.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "santa_groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val budget: String = "",
    /** Exchange date as UTC midnight millis (what the Material date picker produces). */
    val eventDateMillis: Long? = null,
    val location: String = "",
    val notes: String = "",
    /** Set when names have been drawn; null while the group is still a draft. */
    val drawnAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val organizerName: String = "",
    @ColumnInfo(defaultValue = "''") val organizerPhone: String = "",
    @ColumnInfo(defaultValue = "''") val organizerEmail: String = "",
) {
    val isDrawn: Boolean get() = drawnAt != null
}

@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId")],
)
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    /** The participant this person gives a gift to. */
    val assignedToId: Long? = null,
    /** Whether the organizer has sent this person their match. */
    val notified: Boolean = false,
    /** This participant is the organizer, who chose to take part in the draw. */
    @ColumnInfo(defaultValue = "0") val isOrganizer: Boolean = false,
)

/** "[fromId] must not draw [toId]". A two-way rule is stored as two rows. */
@Entity(
    tableName = "exclusions",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("groupId"),
        Index("toId"),
        Index(value = ["fromId", "toId"], unique = true),
    ],
)
data class ExclusionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val fromId: Long,
    val toId: Long,
)

data class GroupSummary(
    @Embedded val group: GroupEntity,
    val participantCount: Int,
    val notifiedCount: Int,
)
