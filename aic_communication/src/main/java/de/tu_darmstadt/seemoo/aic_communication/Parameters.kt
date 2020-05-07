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

import android.media.AudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioFormat

const val RECORDER_SAMPLERATE = 44_100 // According to specs "guaranteed to work on all devices"
const val RECORDER_CHANNELS =
    AudioFormat.CHANNEL_IN_MONO // According to specs "guaranteed to work on all devices"
const val RECORDER_AUDIO_ENCODING =
    AudioFormat.ENCODING_PCM_16BIT // According to specs "guaranteed to work on all devices"
const val RECORDER_BUFFER_SIZE = 20_240 // number of shorts to buffer
const val RECORDER_BUFFER_BYTES_PER_ELEMENT = 2 // 2 bytes in 16bit format
//const val THRESHOLD_ONE_DB = -74 // Threshold to detect a 1, in dB
//private const val THRESHOLD_ZERO_DB = -70 // Threshold to detect a 1, in dB
const val TX_GAIN = 5000 // Factor to increase transmit power
const val THRESHOLD_SAMPLE = 1e-4f // Threshold when to detect an individual sample as one
const val WINDOWING_FACTOR = 0.1 // Factor of each buffer's transition phase at beginning and end
val AUDIO_FORMAT = TarsosDSPAudioFormat(RECORDER_SAMPLERATE.toFloat(), 16, 1, true, false)
var CALIBRATION_DATA = intArrayOf(
    1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1,
    1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1,
    1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1
)


class Parameters(
    val fs: Int, val timePerSlot: Float, val fLow: Int, val fHigh: Int,
    val noiseFloorPower: Int, val snr: Int, val N: Int
) {
    val delimiter = intArrayOf(1, 1, 1, 0, 0, 0)
    val timePerSymbol = 2 * timePerSlot
    val samplesPerSlot: Int = (timePerSlot * fs).toInt() // TODO: Rounding error?
    val samplesPerSymbol = 2 * samplesPerSlot
    val bandwith = fHigh - fLow
    val samplesPerDelimiter = delimiter.size * samplesPerSlot
    val symbolsPerFrame = N
    val slotsPerFrame = 2 * symbolsPerFrame
    val samplesPerFrame = samplesPerSymbol * symbolsPerFrame
    val timePerFrame = timePerSlot * symbolsPerFrame
    val thresholdHigh = noiseFloorPower + 11
    val thresholdLow = noiseFloorPower + 11
    val bufferSize: Int = (2 * samplesPerDelimiter + 2 * samplesPerFrame).toInt()
}

