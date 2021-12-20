package net.auoeke.testmod

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.fabricmc.api.ModInitializer

class Serialization : ModInitializer {
    @Serializable
    data class Mod(val schemaVersion: Int, val id: String, val name: String? = null, val description: String? = null)

    override fun onInitialize() {
        val json = Json {ignoreUnknownKeys = true}
        Serialization::class.java.classLoader.resources("fabric.mod.json").map {json.decodeFromString<Mod>(it.readText())}.forEach(::println)
    }
}
