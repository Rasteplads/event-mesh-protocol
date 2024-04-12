package rasteplads.util.byte_array_extension

import java.util.Collections

fun cartesianProduct(i: Int): List<List<Byte>> =
    when (i) {
        0 -> emptyList()
        1 -> listOf(listOf(Byte.MAX_VALUE), listOf(Byte.MIN_VALUE))
        else -> {
            val r = cartesianProduct(i - 1)
            listOf(Byte.MAX_VALUE, Byte.MIN_VALUE).flatMap { b ->
                r.map { l ->
                    buildList {
                        this.add(b)
                        this.addAll(l)
                    }
                }
            }
        }
    }

fun <T> List<T>.permutations(): List<List<T>> {
    val solutions = mutableListOf<List<T>>()
    permutationsRecursive(this, 0, solutions)
    return solutions
}

fun <T> permutationsRecursive(input: List<T>, index: Int, answers: MutableList<List<T>>) {
    if (index == input.lastIndex) answers.add(input.toList())
    for (i in index..input.lastIndex) {
        Collections.swap(input, index, i)
        permutationsRecursive(input, index + 1, answers)
        Collections.swap(input, i, index)
    }
}

fun generateRands(num: Int): List<Byte> =
    List(num) { (Byte.MIN_VALUE..Byte.MAX_VALUE).random().toByte() }
