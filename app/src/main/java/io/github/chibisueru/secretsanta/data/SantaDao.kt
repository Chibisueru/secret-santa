package io.github.chibisueru.secretsanta.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SantaDao {

    // ---- Groups ----

    @Query(
        """
        SELECT g.*,
            (SELECT COUNT(*) FROM participants p WHERE p.groupId = g.id) AS participantCount,
            (SELECT COUNT(*) FROM participants p WHERE p.groupId = g.id AND p.notified = 1) AS notifiedCount
        FROM santa_groups g
        ORDER BY g.createdAt DESC
        """,
    )
    abstract fun observeGroups(): Flow<List<GroupSummary>>

    @Query("SELECT * FROM santa_groups WHERE id = :id")
    abstract fun observeGroup(id: Long): Flow<GroupEntity?>

    @Insert
    abstract suspend fun insertGroup(group: GroupEntity): Long

    @Query(
        """
        UPDATE santa_groups
        SET name = :name, budget = :budget, eventDateMillis = :eventDateMillis,
            location = :location, notes = :notes
        WHERE id = :id
        """,
    )
    abstract suspend fun updateDetails(
        id: Long,
        name: String,
        budget: String,
        eventDateMillis: Long?,
        location: String,
        notes: String,
    )

    @Query("DELETE FROM santa_groups WHERE id = :id")
    abstract suspend fun deleteGroup(id: Long)

    @Query("SELECT * FROM santa_groups ORDER BY createdAt DESC LIMIT 1")
    abstract suspend fun latestGroup(): GroupEntity?

    @Query(
        """
        UPDATE santa_groups
        SET organizerName = :name, organizerPhone = :phone, organizerEmail = :email
        WHERE id = :id
        """,
    )
    protected abstract suspend fun setOrganizerDetails(id: Long, name: String, phone: String, email: String)

    /** Creates the group and, when the organizer takes part, adds them as a participant. */
    @Transaction
    open suspend fun createGroup(group: GroupEntity, organizerParticipates: Boolean): Long {
        val id = insertGroup(group)
        if (organizerParticipates) {
            insertParticipant(
                ParticipantEntity(
                    groupId = id,
                    name = group.organizerName,
                    phone = group.organizerPhone,
                    email = group.organizerEmail,
                    isOrganizer = true,
                ),
            )
        }
        return id
    }

    /** Updates the organizer's details, including their participant row if they take part. */
    @Transaction
    open suspend fun updateOrganizer(groupId: Long, name: String, phone: String, email: String) {
        setOrganizerDetails(groupId, name, phone, email)
        getOrganizerParticipant(groupId)?.let {
            updateParticipant(it.copy(name = name, phone = phone, email = email))
        }
    }

    @Transaction
    open suspend fun setOrganizerParticipates(groupId: Long, participates: Boolean) {
        val current = getOrganizerParticipant(groupId)
        if (!participates) {
            current?.let { deleteParticipant(it) }
            return
        }
        if (current != null) return
        val group = getGroup(groupId) ?: return
        if (group.organizerName.isBlank()) return
        // If they were already added by name, mark that person as the organizer instead.
        val sameName = getParticipants(groupId).firstOrNull {
            it.name.trim().equals(group.organizerName.trim(), ignoreCase = true)
        }
        if (sameName != null) {
            updateParticipant(
                sameName.copy(
                    isOrganizer = true,
                    phone = sameName.phone.ifBlank { group.organizerPhone },
                    email = sameName.email.ifBlank { group.organizerEmail },
                ),
            )
        } else {
            insertParticipant(
                ParticipantEntity(
                    groupId = groupId,
                    name = group.organizerName,
                    phone = group.organizerPhone,
                    email = group.organizerEmail,
                    isOrganizer = true,
                ),
            )
        }
    }

    @Query("SELECT * FROM santa_groups WHERE id = :id")
    protected abstract suspend fun getGroup(id: Long): GroupEntity?

    @Query("UPDATE santa_groups SET drawnAt = :drawnAt WHERE id = :id")
    protected abstract suspend fun setDrawnAt(id: Long, drawnAt: Long?)

    // ---- Participants ----

    @Query("SELECT * FROM participants WHERE groupId = :groupId ORDER BY name COLLATE NOCASE")
    abstract fun observeParticipants(groupId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM participants WHERE groupId = :groupId")
    abstract suspend fun getParticipants(groupId: Long): List<ParticipantEntity>

    @Query("SELECT * FROM participants WHERE groupId = :groupId AND isOrganizer = 1 LIMIT 1")
    abstract suspend fun getOrganizerParticipant(groupId: Long): ParticipantEntity?

    @Insert
    abstract suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Update
    abstract suspend fun updateParticipant(participant: ParticipantEntity)

    @Delete
    abstract suspend fun deleteParticipant(participant: ParticipantEntity)

    @Query("UPDATE participants SET notified = :notified WHERE id = :id")
    abstract suspend fun setNotified(id: Long, notified: Boolean)

    @Query("UPDATE participants SET assignedToId = :assignedToId, notified = 0 WHERE id = :id")
    protected abstract suspend fun setAssignment(id: Long, assignedToId: Long?)

    @Query("UPDATE participants SET assignedToId = NULL, notified = 0 WHERE groupId = :groupId")
    protected abstract suspend fun clearAssignments(groupId: Long)

    // ---- Exclusions ----

    @Query("SELECT * FROM exclusions WHERE groupId = :groupId")
    abstract fun observeExclusions(groupId: Long): Flow<List<ExclusionEntity>>

    @Query("SELECT * FROM exclusions WHERE groupId = :groupId")
    abstract suspend fun getExclusions(groupId: Long): List<ExclusionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertExclusions(exclusions: List<ExclusionEntity>)

    @Query(
        """
        DELETE FROM exclusions
        WHERE (fromId = :a AND toId = :b) OR (fromId = :b AND toId = :a)
        """,
    )
    abstract suspend fun deleteExclusionPair(a: Long, b: Long)

    @Query("DELETE FROM exclusions WHERE fromId = :fromId AND toId = :toId")
    abstract suspend fun deleteExclusion(fromId: Long, toId: Long)

    // ---- Draw ----

    @Transaction
    open suspend fun saveDraw(groupId: Long, assignments: Map<Long, Long>) {
        clearAssignments(groupId)
        assignments.forEach { (giver, receiver) -> setAssignment(giver, receiver) }
        setDrawnAt(groupId, System.currentTimeMillis())
    }

    @Transaction
    open suspend fun resetDraw(groupId: Long) {
        clearAssignments(groupId)
        setDrawnAt(groupId, null)
    }
}
