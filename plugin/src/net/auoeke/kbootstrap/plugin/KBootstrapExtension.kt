package net.auoeke.kbootstrap.plugin

class KBootstrapExtension {
    internal val modules: MutableSet<String> = HashSet()

    fun modules(vararg names: String) {
        names.forEach {
            if (it !in allowedModules) {
                throw IllegalArgumentException(""""$it" does not name a valid module (any of $allowedModules).""")
            }

            this.modules += it
        }
    }

    fun allModules() {
        this.modules += allowedModules
    }

    companion object {
        private val allowedModules: Array<String> = arrayOf("coroutines", "reflect")
    }
}
