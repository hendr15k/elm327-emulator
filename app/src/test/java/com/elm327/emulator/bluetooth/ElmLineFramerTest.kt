package com.elm327.emulator.bluetooth

import org.junit.Assert.*
import org.junit.Test

class ElmLineFramerTest {

    @Test
    fun splitsMultipleCommandsInOnePacket() {
        val framer = ElmLineFramer()
        val lines = framer.feed("ATE0\rATI\r010C\r".asIterable())
        assertEquals(listOf("ATE0", "ATI", "010C"), lines)
    }

    @Test
    fun assemblesCommandSplitAcrossPackets() {
        val framer = ElmLineFramer()
        assertEquals(emptyList<String>(), framer.feed("AT".asIterable()))
        assertEquals(listOf("ATI"), framer.feed("I\r".asIterable()))
    }

    @Test
    fun treatsBareCarriageReturnAsEmptyLine() {
        val framer = ElmLineFramer()
        assertEquals(listOf(""), framer.feed("\r".asIterable()))
    }

    @Test
    fun ignoresLfWithoutCr() {
        val framer = ElmLineFramer()
        assertEquals(emptyList<String>(), framer.feed("\n".asIterable()))
        assertEquals(listOf("ATI"), framer.feed("ATI\n".asIterable()))
    }

    @Test
    fun handlesCrLfPairs() {
        val framer = ElmLineFramer()
        assertEquals(listOf("ATI", "ATE0"), framer.feed("ATI\r\nATE0\r\n".asIterable()))
    }

    @Test
    fun dropsGarbageWithoutTerminator() {
        val framer = ElmLineFramer()
        framer.feed("x".repeat(300).asIterable())
        assertEquals(emptyList<String>(), framer.feed("\r".asIterable()))
    }
}
