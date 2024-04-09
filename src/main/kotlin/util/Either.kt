package org.rasteplads.util

final class Either<L, R>
private constructor(private val left: L?, private val right: R?, private val which: LR) {
    private enum class LR {
        L,
        R
    }

    fun isLeft(): Boolean = which == LR.L

    fun isRight(): Boolean = which == LR.R

    fun getLeft(): L? = left

    fun getRight(): R? = right

    companion object {
        fun <L, R> left(value: L): Either<L, R> = Either(value, null, LR.L)

        fun <L, R> right(value: R): Either<L, R> = Either(null, value, LR.R)
    }
}
