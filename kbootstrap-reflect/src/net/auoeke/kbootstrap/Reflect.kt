package net.auoeke.kbootstrap

import java.util.function.BooleanSupplier

object Reflect : BooleanSupplier {
    override fun getAsBoolean(): Boolean = KBootstrap.download(false, "reflect", "kotlin.reflect.jvm.KClassesJvm")
}
