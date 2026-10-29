package com.elm327.emulator.bluetooth

/**
 * Splits the incoming RFCOMM byte stream into ELM327 command lines.
 *
 * A client may pack several commands into one packet ("ATE0\rATI\r") and may
 * also split one command across packets. Treating each packet as a single
 * command therefore both merges commands into unparseable garbage and fails on
 * packets that end mid-command.
 */
internal class ElmLineFramer {

    private val buffer = StringBuilder()
    private var discarding = false

    fun feed(chunk: Iterable<Char>): List<String> {
        val lines = ArrayList<String>()
        for (ch in chunk) {
            when (ch) {
                '\r' -> if (discarding) discarding = false else flush(lines)
                '\n' -> if (buffer.isNotEmpty()) flush(lines)
                else -> if (!discarding) {
                    buffer.append(ch)
                    if (buffer.length > MAX_LINE_LENGTH) {
                        // Garbage without a terminator: drop the whole line
                        // instead of keeping its tail and growing forever.
                        buffer.setLength(0)
                        discarding = true
                    }
                }
            }
        }
        return lines
    }

    fun reset() {
        buffer.setLength(0)
        discarding = false
    }

    private fun flush(lines: MutableList<String>) {
        lines.add(buffer.toString())
        buffer.setLength(0)
    }

    private companion object {
        const val MAX_LINE_LENGTH = 256
    }
}
