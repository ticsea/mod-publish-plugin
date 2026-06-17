package me.modmuss50.mpp.platforms.xyebbs

object XyebbsGameVersion {
    private val byName = mapOf(
        "1.20.1" to 222,
        "1.21.1" to 224
    )

    private val byId = byName.entries.associate { it.value to it.key }

    fun fromName(name: String): Int? = byName[name]
    fun fromId(id: Int): String? = byId[id]

    // Batch - skips nulls automatically
    fun idsToVersions(ids: List<Int>): List<String> =
        ids.mapNotNull { byId[it] }  // Skips nulls!

    fun versionsToIds(versions: List<String>): List<Int> =
        versions.mapNotNull { byName[it] }  // Skips nulls!
}

object XyebbsLoader {
    private val byName = mapOf (
        "fabric" to 25,
        "forge" to 24,
        "neoforge" to 38
    )

    private val byId = byName.entries.associate { it.value to it.key }

    fun fromName(name: String): Int? = byName[name]
    fun fromId(id: Int): String? = byId[id]

    // Batch - skips nulls automatically
    fun idsToLoader(ids: List<Int>): List<String> =
        ids.mapNotNull { byId[it] }  // Skips nulls!

    fun loadersToId(versions: List<String>): List<Int> =
        versions.mapNotNull { byName[it] }  // Skips nulls!
}