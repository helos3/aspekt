abstract class Aspekt<V, R> {
    abstract val before: ((V) -> Unit)?
    abstract val after: ((R) -> Unit)?
    abstract val afterThrowing: ((Exception) -> Unit)?
    abstract val around: ((V) -> R?)?
}

