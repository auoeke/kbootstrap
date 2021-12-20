package net.auoeke.kbootstrap.plugin

import org.gradle.api.Action
import org.gradle.api.Task

fun <T> Iterable<T>.withEach(action: T.() -> Unit) {
    forEach {it.run(action)}
}

@Suppress("UNCHECKED_CAST")
fun <T : Task> T.first(action: (T) -> Unit): T = doFirst(taskAction(action)) as T

@Suppress("UNCHECKED_CAST")
fun <T : Task> T.last(action: (T) -> Unit): T = doLast(taskAction(action)) as T

@Suppress("ObjectLiteralToLambda", "UNCHECKED_CAST")
fun <T> taskAction(action: (T) -> Unit): Action<Task> = object : Action<Task> {
    override fun execute(obj: Task) {
        action(obj as T)
    }
}
