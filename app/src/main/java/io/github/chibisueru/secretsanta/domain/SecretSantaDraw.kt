package io.github.chibisueru.secretsanta.domain

import kotlin.random.Random

sealed interface DrawResult {
    /** giver id -> receiver id */
    data class Success(val assignments: Map<Long, Long>) : DrawResult
    data class Failure(val reason: String) : DrawResult
}

data class DrawPerson(val id: Long, val name: String)

/** "[fromId] must not draw [toId]" */
data class DrawRule(val fromId: Long, val toId: Long)

object SecretSantaDraw {
    const val MIN_PARTICIPANTS = 3
    private const val RANDOM_ATTEMPTS = 2_000

    fun draw(
        people: List<DrawPerson>,
        rules: List<DrawRule>,
        random: Random = Random.Default,
    ): DrawResult {
        if (people.size < MIN_PARTICIPANTS) {
            return DrawResult.Failure(
                "Add at least $MIN_PARTICIPANTS participants. With fewer, everyone could work out who has whom.",
            )
        }

        val ids = people.map { it.id }
        val names = people.associate { it.id to it.name }
        val blocked: Map<Long, Set<Long>> =
            rules.groupBy({ it.fromId }, { it.toId }).mapValues { it.value.toSet() }
        val allowed: Map<Long, List<Long>> = ids.associateWith { giver ->
            ids.filter { it != giver && it !in blocked[giver].orEmpty() }
        }

        // Point at the specific person first: it's the most useful explanation.
        allowed.entries.firstOrNull { it.value.isEmpty() }?.let { (giver, _) ->
            return DrawResult.Failure(
                "${names[giver]} isn't allowed to draw anyone. Remove some of their rules.",
            )
        }
        ids.firstOrNull { receiver -> ids.none { receiver in allowed.getValue(it) } }?.let { receiver ->
            return DrawResult.Failure(
                "Nobody is allowed to draw ${names[receiver]}. Remove some rules involving them.",
            )
        }

        // 1) Shuffle the whole list until one fits. Every valid result is equally likely.
        repeat(RANDOM_ATTEMPTS) {
            val receivers = ids.shuffled(random)
            if (ids.indices.all { i -> receivers[i] in allowed.getValue(ids[i]) }) {
                return DrawResult.Success(ids.zip(receivers).toMap())
            }
        }

        // 2) Heavily constrained groups: randomized backtracking, which always finds a
        //    solution if one exists.
        return backtrack(ids, allowed, random)?.let { DrawResult.Success(it) }
            ?: DrawResult.Failure(
                "No valid draw is possible with these rules. Try removing some exclusions.",
            )
    }

    private fun backtrack(
        ids: List<Long>,
        allowed: Map<Long, List<Long>>,
        random: Random,
    ): Map<Long, Long>? {
        // Most-constrained givers first keeps the search small.
        val order = ids.shuffled(random).sortedBy { allowed.getValue(it).size }
        val taken = HashSet<Long>()
        val result = HashMap<Long, Long>()

        fun solve(index: Int): Boolean {
            if (index == order.size) return true
            val giver = order[index]
            for (receiver in allowed.getValue(giver).shuffled(random)) {
                if (!taken.add(receiver)) continue
                result[giver] = receiver
                if (solve(index + 1)) return true
                taken.remove(receiver)
                result.remove(giver)
            }
            return false
        }

        return if (solve(0)) result else null
    }
}
