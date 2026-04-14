package com.blueprint.cubing.core.parse

import com.blueprint.cubing.core.logic.toKociembaFacelets
import com.blueprint.cubing.core.model.CubeEvent
import com.blueprint.cubing.core.model.CubePermState
import com.blueprint.cubing.log.Logger
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalTime::class)
class Gen2MessageParser {

    private class Impl(
        override val CP: Array<Int>,
        override val CO: Array<Int>,
        override val EP: Array<Int>,
        override val EO: Array<Int>
    ) : CubePermState


    // Состояние для парсинга между событиями
    private var lastSerial: Int = -1

    // Храним последний известный стейт куба; по умолчанию решённый
    private fun solvedState(): Impl = Impl(
        CP = Array(8) { it },
        CO = Array(8) { 0 },
        EP = Array(12) { it },
        EO = Array(12) { 0 }
    )

    var lastState: CubePermState = solvedState()
    private set

    fun parseEvent(bytes: ByteArray): CubeEvent {
        return parseMessage(bytes)
    }

    //    private val moveChars = arrayOf("U", "B", "R", "D", "F", "L", )
    private val moveChars = arrayOf("U", "R", "F", "D", "L", "B")

    /**
     * Parse cube state from raw bytes. If deviceMac is provided, use it to build encryption salt
     * (MAC bytes reversed) as in the web implementation; otherwise try non-MAC heuristics.
     *
     * Реализовано частично: парсит FACELETS-событие и возвращает CubePermState; для остальных событий
     * возвращает последний известный стейт (по-умолчанию решённый).
     */
    @Suppress("UNUSED_PARAMETER")
    private fun parseMessage(bytes: ByteArray): CubeEvent {

        val now = Clock.System.now().toEpochMilliseconds()
        val msg = GanProtocolMessageView(bytes)
        val eventType = msg.getBitWord(0, 4).toInt()

        when (eventType) {
            0x01 -> { // GYRO
                // Orientation Quaternion
                val qw = msg.getBitWord(4, 16).toInt()
                val qx = msg.getBitWord(20, 16).toInt()
                val qy = msg.getBitWord(36, 16).toInt()
                val qz = msg.getBitWord(52, 16).toInt()

                // Angular Velocity (4-bit signed values in JS code)
                val vx = msg.getBitWord(68, 4).toInt()
                val vy = msg.getBitWord(72, 4).toInt()
                val vz = msg.getBitWord(76, 4).toInt()

                // Для текущей реализации — логируем значения, состояние куба не меняется
                Logger.log("MessageParser", "GYRO qw=$qw qx=$qx qy=$qy qz=$qz vx=$vx vy=$vy vz=$vz")
                return CubeEvent.Unsupported
            }

            0x02 -> { // MOVE
                if (lastSerial != -1) {
                    val serial = msg.getBitWord(4, 8).toInt()
                    val diff = min(((serial - lastSerial) and 0xFF), 7)

                    lastSerial = serial
                    var moveStr = ""
                    var elapsed = 0L
                    if (diff > 0) {
                        for (i in (diff - 1) downTo 0) {
                            val face = msg.getBitWord(12 + 5 * i, 4).toInt()
                            val direction = msg.getBitWord(16 + 5 * i, 1).toInt()
                            elapsed = msg.getBitWord(47 + 16 * i, 16)

                            if(i != 0) {
                                Logger.log("MessageParser", "Missed and recovered events")
                            }
                            Logger.log(
                                "MessageParser",
                                "MOVE serial=$serial face=$face dir=$direction elapsed=$elapsed"
                            )
                            moveStr += " " + moveChars[face] + if (direction == 1) "'" else ""
                        }

                    }
                    return CubeEvent.Move(moveStr.trim(), elapsed = elapsed)
                }

                return CubeEvent.Unsupported
            }

            0x04 -> { // FACELETS
                val serial = msg.getBitWord(4, 8).toInt()
                if (lastSerial == -1) lastSerial = serial

                val cpList = mutableListOf<Int>()
                val coList = mutableListOf<Int>()
                val epList = mutableListOf<Int>()
                val eoList = mutableListOf<Int>()

                // Corners: 7 items explicit
                for (i in 0 until 7) {
                    val cpv = msg.getBitWord(12 + i * 3, 3).toInt()
                    val cov = msg.getBitWord(33 + i * 2, 2).toInt()
                    cpList.add(cpv)
                    coList.add(cov)
                }
                // last corner derived
                cpList.add(28 - sumInts(cpList))
                coList.add((3 - (sumInts(coList) % 3)) % 3)

                // Edges: 11 items explicit
                for (i in 0 until 11) {
                    val epv = msg.getBitWord(47 + i * 4, 4).toInt()
                    val eov = msg.getBitWord(91 + i, 1).toInt()
                    epList.add(epv)
                    eoList.add(eov)
                }
                epList.add(66 - sumInts(epList))
                eoList.add((2 - (sumInts(eoList) % 2)) % 2)

                // Обновляем внутреннее состояние
                lastSerial = serial

                val cpArray = Array(8) { idx -> cpList[idx] }
                val coArray = Array(8) { idx -> coList[idx] }
                val epArray = Array(12) { idx -> epList[idx] }
                val eoArray = Array(12) { idx -> eoList[idx] }

                val newState = Impl(
                    CP = cpArray,
                    CO = coArray,
                    EP = epArray,
                    EO = eoArray
                )

                lastState = newState
                return CubeEvent.CubeStateUpdated(
                    state = newState,
                    kociembaState = toKociembaFacelets(cpArray, coArray, epArray, eoArray),
                )
            }

            0x05 -> { // HARDWARE
                val hwMajor = msg.getBitWord(8, 8).toInt()
                val hwMinor = msg.getBitWord(16, 8).toInt()
                val swMajor = msg.getBitWord(24, 8).toInt()
                val swMinor = msg.getBitWord(32, 8).toInt()
                val gyroSupported = msg.getBitWord(104, 1).toInt()

                val sb = StringBuilder()
                for (i in 0 until 8) {
                    val ch = msg.getBitWord(i * 8 + 40, 8).toInt()
                    sb.append(ch.toChar())
                }
                val hardwareName = sb.toString()

                Logger.log(
                    "MessageParser",
                    "HARDWARE name=$hardwareName hw=$hwMajor.$hwMinor sw=$swMajor.$swMinor gyro=$gyroSupported"
                )
                return CubeEvent.Unsupported
            }

            0x09 -> { // BATTERY
                val batteryLevel = msg.getBitWord(8, 8).toInt()
                Logger.log("MessageParser", "BATTERY level=$batteryLevel")
                return CubeEvent.Unsupported
            }

            0x0D -> { // DISCONNECT
                // conn.disconnect() in original code — skipped as requested
                Logger.log("MessageParser", "DISCONNECT event received")
                return CubeEvent.Unsupported
            }

            else -> return CubeEvent.Unsupported
        }
    }

    private fun sumInts(list: List<Int>): Int {
        var s = 0
        for (v in list) s += v
        return s
    }

}

