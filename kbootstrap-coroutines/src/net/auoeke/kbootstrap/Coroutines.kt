package net.auoeke.kbootstrap

import java.util.function.BooleanSupplier

object Coroutines : BooleanSupplier {
    override fun getAsBoolean(): Boolean = arrayOf("coroutines-core", "coroutines-core-jvm", "coroutines-jdk8", "coroutines-jdk9")
        .map {KBootstrap.download(true, it)}
        .reduce {first, second -> first or second}
}
