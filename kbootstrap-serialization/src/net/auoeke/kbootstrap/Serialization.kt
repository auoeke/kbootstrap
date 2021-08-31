package net.auoeke.kbootstrap

import java.util.function.BooleanSupplier

object Serialization : BooleanSupplier {
    override fun getAsBoolean(): Boolean = KBootstrap.download(true, "serialization-core-jvm", "kotlinx.serialization.Serializable") or KBootstrap.download(true, "serialization-json-jvm", "kotlinx.serialization.json.JsonElement")
}
