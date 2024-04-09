package org.example

import org.example.api.FatClass

fun main() {
    FatClass.builder<Int, Byte>()
        .withDataConstant(0)
        //.setFromIDFunction { i -> i.toBUyteArray() }
        .build()
    println("Hello World!")
}
