package net.auoeke.testmod

import net.fabricmc.api.ModInitializer

private class Main : ModInitializer {
    private val mainInitializer: ModInitializer = ModInitializer {println("hello from field initializer")}

    override fun onInitialize() = println("hello from onInitialize")
    fun init() = println("hello from init")

    companion object NamedCompanion : ModInitializer {
        @JvmStatic
        val staticInitializer: ModInitializer = ModInitializer {println("hello from companion static field initializer")}

        private val initializer: ModInitializer = ModInitializer {println("hello from companion object field initializer")}

        @JvmStatic
        private fun staticInit() = println("hello from companion static init")

        override fun onInitialize() = println("hello from companion object onInitialize")
        fun init() = println("hello from companion object non-static init")
    }
}

private object Object : ModInitializer {
    @JvmStatic
    private val staticInitializer: ModInitializer = ModInitializer {println("hello from static field initializer")}

    private val initializer: ModInitializer = ModInitializer {println("hello from object field initializer")}

    @JvmStatic
    private fun staticInit() = println("hello from static init")

    override fun onInitialize() = println("hello from object onInitialize")
    private fun init() = println("hello from object non-static init")
}
