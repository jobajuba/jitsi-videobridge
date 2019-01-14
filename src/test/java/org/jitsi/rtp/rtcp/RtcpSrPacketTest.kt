/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.rtp.rtcp

import io.kotlintest.matchers.containAll
import io.kotlintest.matchers.haveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.extensions.toHex
import java.nio.ByteBuffer
import kotlin.math.exp

internal class RtcpSrPacketTest : ShouldSpec() {
    override fun isInstancePerTest(): Boolean = true

    private val expectedSenderInfo = SenderInfo(
        ntpTimestamp = 0x7FFFFFFFFFFFFFFF,
        rtpTimestamp = 0xFFFFFFFF,
        sendersPacketCount = 0xFFFFFFFF,
        sendersOctetCount = 0xFFFFFFFF
    )

    private val reportBlock1 = RtcpReportBlock(
        ssrc = 12345,
        fractionLost = 42,
        cumulativePacketsLost = 4242,
        seqNumCycles = 1,
        seqNum = 42,
        interarrivalJitter = 4242,
        lastSrTimestamp = 23456,
        delaySinceLastSr = 34567
    )
    private val reportBlock2 = RtcpReportBlock(
        ssrc = 23456,
        fractionLost = 42,
        cumulativePacketsLost = 4242,
        seqNumCycles = 1,
        seqNum = 42,
        interarrivalJitter = 4242,
        lastSrTimestamp = 23456,
        delaySinceLastSr = 34567
    )

    private val lengthValue =
            (RtcpHeader.SIZE_BYTES + SenderInfo.SIZE_BYTES + RtcpReportBlock.SIZE_BYTES + RtcpReportBlock.SIZE_BYTES + 3) / 4 - 1

    private val expectedHeader = RtcpHeader(
            version = 2,
            hasPadding = false,
            reportCount = 2,
            packetType = 200,
            length = lengthValue,
            senderSsrc = 12345
    )


    init {
        "creation" {
            "from a buffer" {
                val buf = ByteBuffer.allocate(1024)
                buf.put(expectedHeader.getBuffer())
                buf.put(expectedSenderInfo.getBuffer())
                buf.put(reportBlock1.getBuffer())
                buf.put(reportBlock2.getBuffer())
                buf.rewind()
                val srPacket = RtcpSrPacket(buf)
                should("read everything correctly") {
                    srPacket.senderInfo.ntpTimestamp shouldBe expectedSenderInfo.ntpTimestamp
                    srPacket.senderInfo.compactedNtpTimestamp shouldBe 0xFFFFFFFF
                    srPacket.senderInfo.rtpTimestamp shouldBe expectedSenderInfo.rtpTimestamp
                    srPacket.senderInfo.sendersPacketCount shouldBe expectedSenderInfo.sendersPacketCount
                    srPacket.senderInfo.sendersOctetCount shouldBe expectedSenderInfo.sendersOctetCount
                    srPacket.reportBlocks should haveSize(2)
                    srPacket.reportBlocks[0] shouldBe reportBlock1
                    srPacket.reportBlocks[1] shouldBe reportBlock2
                }
            }
            "from explicit values" {
                val srPacket = RtcpSrPacket(
                    header = expectedHeader,
                    senderInfo = expectedSenderInfo,
                    reportBlocks = mutableListOf(
                        reportBlock1,
                        reportBlock2
                    )
                )
                should("set all values correctly") {
                    srPacket.header shouldBe expectedHeader
                    srPacket.senderInfo shouldBe expectedSenderInfo
                    srPacket.reportBlocks should containAll(reportBlock1, reportBlock2)
                }
            }
            "from an incomplete set of values" {
                val srPacket = RtcpSrPacket(
                    header = RtcpHeader(senderSsrc = 12345),
                    senderInfo = expectedSenderInfo,
                    reportBlocks = mutableListOf(
                            reportBlock1,
                            reportBlock2
                    )
                )
                val parsedPacket = RtcpSrPacket(srPacket.getBuffer())
                should("set all values correctly") {
                    parsedPacket.header shouldBe expectedHeader
                    parsedPacket.senderInfo shouldBe expectedSenderInfo
                    parsedPacket.reportBlocks should containAll(reportBlock1, reportBlock2)
                }
            }
        }
        "serialization" {
            val expectedBuf = ByteBuffer.allocate(1024)
            expectedBuf.put(expectedHeader.getBuffer())
            expectedBuf.put(expectedSenderInfo.getBuffer())
            expectedBuf.put(reportBlock1.getBuffer())
            expectedBuf.put(reportBlock2.getBuffer())
            expectedBuf.position(0)
            val srPacket = RtcpSrPacket(expectedBuf)

            val actualBuf = srPacket.getBuffer()
            should("write all values correctly") {
                for (i in 0 until actualBuf.limit()) {
                    actualBuf.get(i) shouldBe expectedBuf.get(i)
                }
            }
        }
    }
}
