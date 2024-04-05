package org.example

import org.example.api.FatClass

fun main() {
    FatClass.builder<Int, Byte>()
        .setDataConstant(0)
        //.setFromIDFunction { i -> i.toBUyteArray() }
        .build()
}
