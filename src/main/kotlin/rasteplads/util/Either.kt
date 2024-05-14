package rasteplads.util

class Either<L, R> private constructor(private val value: Any?, private val which: LR) {
    private enum class LR {
        L,
        R
    }

    fun isLeft(): Boolean = which == LR.L

    fun isRight(): Boolean = which == LR.R

    fun getLeft(): L? = if (which == LR.L) value as L else null

    fun getRight(): R? = if (which == LR.R) value as R else null

    companion object {
        fun <L, R> left(value: L): Either<L, R> = Either(value, LR.L)

        fun <L, R> right(value: R): Either<L, R> = Either(value, LR.R)
    }
}
