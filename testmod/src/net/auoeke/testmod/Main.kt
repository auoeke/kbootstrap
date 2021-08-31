package net.auoeke.testmod

import net.fabricmc.api.ModInitializer

class Main : ModInitializer {
    override fun onInitialize() {
        repeat(5) {
            println("Hello world from Kotlin $it")
        }
    }
}
