package rasteplads.util.byte_array_extension

import java.util.Collections

fun cartesianProduct(i: Int): List<List<Byte>> =
    if (i == 0) emptyList()
    else if (i == 1)
    // emptyList()
    listOf(listOf(Byte.MAX_VALUE), listOf(Byte.MIN_VALUE))
    else {
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

fun <T> getPermutationsWithDistinctValues(original: List<T>): Set<List<T>> {
    if (original.isEmpty()) return emptySet()
    val permutationInstructions =
        original
            .toSet()
            .map { it to original.count { x -> x == it } }
            .fold(listOf(setOf<Pair<T, Int>>())) { acc, (value, valueCount) ->
                mutableListOf<Set<Pair<T, Int>>>().apply {
                    for (set in acc) for (retainIndex in 0 until valueCount) add(
                        set + (value to retainIndex))
                }
            }
    return mutableSetOf<List<T>>().also { outSet ->
        for (instructionSet in permutationInstructions) {
            outSet +=
                original.toMutableList().apply {
                    for ((value, retainIndex) in instructionSet) {
                        repeat(retainIndex) { removeAt(indexOfFirst { it == value }) }
                        repeat(count { it == value } - 1) { removeAt(indexOfLast { it == value }) }
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
