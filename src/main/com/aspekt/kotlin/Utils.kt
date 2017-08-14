inline fun <E> Iterable<E>.peek(action: (E) -> Unit): Iterable<E> {
    for (element in this) action(element)
    return this
}
