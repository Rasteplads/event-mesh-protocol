package rasteplads.bluetooth

import java.util.ArrayDeque

class MessageBuffer<T>{
    val buffer = ArrayDeque<Message<T>>()
}