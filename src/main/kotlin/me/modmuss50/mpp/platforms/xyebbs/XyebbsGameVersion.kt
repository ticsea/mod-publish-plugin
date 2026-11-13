package me.modmuss50.mpp.platforms.xyebbs

object XyebbsGameVersion {
    private val byName = mapOf(
        "1.20.1" to 222,
        "1.21.1" to 224,
        "1.21.2" to 282,
        "1.21.3" to 283,
        "1.21.4" to 284,
        "1.21.9" to 613,
        "1.21.10" to 614,
        "1.21.11" to 620,
        "26.1" to 633,
        "26.1.1" to 634,
        "26.1.2" to 635,
        "26.2" to 646,
        "26.3" to 652,
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