package net.auoeke.kbootstrap.plugin

fun <T> Iterable<T>.withEach(action: T.() -> Unit) {
    this.forEach {it.run(action)}
}
