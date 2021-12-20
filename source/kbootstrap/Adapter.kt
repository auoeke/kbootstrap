package kbootstrap

import net.fabricmc.loader.api.*
import java.lang.invoke.*
import java.lang.reflect.*
import java.util.stream.*

@Suppress("UNCHECKED_CAST", "unused")
internal class Adapter : LanguageAdapter {
    override fun <T> create(mod: ModContainer?, value: String, type: Class<T>): T = this.create(value, type)

    private fun companionField(companionType: Class<*>): Field? {
        companionType.declaringClass?.also {declaringType ->
            val staticFinal = Modifier.STATIC or Modifier.FINAL

            for (field in declaringType.declaredFields) {
                if (field.type === companionType && field.modifiers and staticFinal == staticFinal) {
                    return field
                }
            }
        }

        return null
    }

    private fun <T> findInstance(type: Class<T>): T? {
        val field: Field = try {
            type.getDeclaredField("INSTANCE")
        } catch (exception: NoSuchFieldException) {
            companionField(type)
        }?.apply {trySetAccessible()} ?: return null

        return field[null] as T?
    }

    private fun findField(target: Class<*>, name: String): Field {
        var field: Field

        run {
            try {
                field = target.getDeclaredField(name)
            } catch (exception: NoSuchFieldException) {
                val staticFinal = Modifier.STATIC or Modifier.FINAL

                if (target.modifiers and staticFinal == staticFinal) {
                    try {
                        field = target.declaringClass.getDeclaredField(name)

                        if (field.modifiers and staticFinal == staticFinal) {
                            return@run
                        }
                    } catch (ignored: NoSuchFieldException) {}
                }

                throw exception
            }
        }

        if (field.modifiers and Modifier.FINAL == 0) {
            throw NoSuchFieldException("""Field "$target#$name" is not final.""")
        }

        return field.apply {trySetAccessible()}
    }

    private fun <T> instantiate(type: Class<T>): T = findInstance(type) ?: type.getDeclaredConstructor().apply {trySetAccessible()}.newInstance() as T

    private fun <T> create(value: String, type: Class<T>? = null): T {
        var components: Array<String> = value.split("::".toRegex()).toTypedArray()

        if (components.size > 2) {
            components = arrayOf(Stream.of(*components).limit((components.size - 1).toLong()).collect(Collectors.joining("$")), components[components.size - 1])
        }

        val target = Class.forName(components[0]) as Class<T>

        if (!target.isAnnotationPresent(Metadata::class.java)) {
            throw LanguageAdapterException(target.name + " is not a Kotlin class.")
        }

        if (components.size == 1) {
            return instantiate(target)
        }

        return try {
            val method = target.getDeclaredMethod(components[1])
            var handle = MethodHandles.privateLookupIn(target, MethodHandles.lookup()).unreflect(method)

            if (!Modifier.isStatic(method.modifiers)) {
                handle = handle.bindTo(instantiate(target))
            }

            MethodHandleProxies.asInterfaceInstance(type, handle)
        } catch (exception: NoSuchMethodException) {
            val field = findField(target, components[1])

            field[when {
                Modifier.isStatic(field.modifiers) -> null
                else -> instantiate(target)
            }] as T
        }
    }

    init {
        Downloader.download()
    }
}
