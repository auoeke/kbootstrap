package net.auoeke.kbootstrap.plugin

import org.gradle.api.Project

class KBootstrapExtension(val project: Project) {
    internal val modules: MutableSet<String> = HashSet()

    fun modules(vararg names: String) {
        names.forEach {
            if (it !in allowedModules) {
                throw IllegalArgumentException(""""$it" does not name a valid module (any of $allowedModules).""")
            }

            modules += it
        }
    }

    fun allModules() {
        modules += allowedModules
    }

    companion object {
        private val allowedModules: Array<String> = arrayOf("coroutines", "reflect")
    }
}
