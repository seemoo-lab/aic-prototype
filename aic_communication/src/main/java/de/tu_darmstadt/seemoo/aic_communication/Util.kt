/*
 * AIC Protoype: Secure Short-Range Acoustic Communication.
 *
 * Copyright (c) 2020 AIC Team
 * This file is part of the AIC Prototype.
 *
 * The AIC Prototype is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The AIC Prototype is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the AIC Prototype.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package de.tu_darmstadt.seemoo.aic_communication

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.writer.WriterProcessor
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.log10

/*
 * Converts the bits into manchester encoded bits.
 *
 * The resulting array is twice as long.
 *
 *  0 -> low,high
 *  1 -> high,low
 */
fun manchester(bits: IntArray): IntArray {
    val slots = IntArray(2 * bits.size)
    bits.forEachIndexed { index, bit ->
        if (bit == 0) {
            slots[2 * index] = 0
            slots[2 * index + 1] = 1
        } else {
            slots[2 * index] = 1
            slots[2 * index + 1] = 0
        }
    }
    return slots
}


/**
 * Scales and writes the given samples to a WAV file.
 */
fun writeSamplesToWavFile(outputFile: File, samplesToWrite: FloatArray) {
    // Scale the samples to be in the range Tarsos expects
    val sampleMax = samplesToWrite.max() ?: 0f
    val sampleMin = samplesToWrite.min() ?: 0f
    val sampleAbsMax = if (sampleMax > sampleMin) sampleMax else sampleMin

    var buf = FloatArray(samplesToWrite.size)
    samplesToWrite.forEachIndexed { index, sample ->
        buf[index] = sample / sampleAbsMax
    }

    writeBufToWavFile(outputFile, buf)
}

/**
 * Scales and writes the given samples to a WAV file.
 */
fun writeSamplesToWavFile(outputFile: File, samplesToWrite: DoubleArray) {
    // Scale the samples to be in the range Tarsos expects
    val sampleMax = samplesToWrite.max() ?: 0.0
    val sampleMin = samplesToWrite.min() ?: 0.0
    val sampleAbsMax = if (sampleMax > sampleMin) sampleMax else sampleMin

    var buf = FloatArray(samplesToWrite.size)
    samplesToWrite.forEachIndexed { index, sample ->
        buf[index] = (sample / sampleAbsMax).toFloat()
    }

    writeBufToWavFile(outputFile, buf)
}

/**
 * Writes the given samples to a WAV file.
 */
private fun writeBufToWavFile(outputFile: File, buf: FloatArray) {
    val outputRAFile = RandomAccessFile(outputFile, "rw")
    val wp = WriterProcessor(AUDIO_FORMAT, outputRAFile)
    val ae = AudioEvent(AUDIO_FORMAT)
    ae.floatBuffer = buf
    wp.process(ae)
    wp.processingFinished()
}

/**
 * Counts the character differences of both strings.
 *
 * Both strings have to have the same size.
 */
fun countStringDifferences(string1: String, string2: String): Int {
    assert(string1.length == string2.length)
    var differences = 0
    string1.forEachIndexed { index, c ->
        if (c != string2[index]) {
            differences++
        }
    }
    return differences
}

/**
 * Counts the unclear bits of the string.
 *
 * We use an extended "bitstring" with values 0, 1, 2.
 * The value 2 indicates a decoding error ("unclear value").
 */
fun countStringUnclearBits(str: String): Int {
    var unclearbits = 0
    str.forEach { c ->
        if (c == '2') {
            unclearbits++
        }
    }
    return unclearbits
}

/**
 * Stores statistics about an AIC signal.
 *
 * @param onSlots an array containing concatenated samples of on slots.
 * @param offSlots an array containing concatenated samples of off slots.
 */
class ReceivedSlotInformation(val onSlots: DoubleArray, val offSlots: DoubleArray) {
    private fun pow2db(power: Double): Float {
        return 20f * log10(power).toFloat()
    }

    fun onPowerAverage(): Float {
        return pow2db(onSlots.average())
    }

    fun onPowerMinimum(): Float {
        return pow2db(onSlots.min()!!)
    }

    fun onPowerMaximum(): Float {
        return pow2db(onSlots.max()!!)
    }

    fun offPowerAverage(): Float {
        return pow2db(offSlots.average())
    }

    fun offPowerMinimum(): Float {
        return pow2db(offSlots.min()!!)
    }

    fun offPowerMaximum(): Float {
        return pow2db(offSlots.max()!!)
    }

    fun totalPowerAverage(): Float {
        return pow2db((onSlots.average() + offSlots.average()) / 2)
    }
}