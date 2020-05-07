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
import android.media.AudioManager
import android.media.AudioTrack
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.security.SecureRandom


/**
 * Generates and transmits AIC signals corresponding to the given data.
 *
 * The data is repeatedly broadcast using the speaker.
 */
class AICTransmitter(val data: IntArray, val param: Parameters) : Runnable, AnkoLogger {
    var running = true

    override fun run() {
        val slots = param.delimiter + manchester(data)
        val numSamples = param.samplesPerDelimiter + param.samplesPerFrame

        info("Buffer size: $numSamples")
        // Generate noise in this band
        // NOTE: Random() is faster (real-time even on older devices), but potentially vulnerable to signal cancellation.
        // SecureRandom was fast enough in our tests on newer devices (e.g., OnePlus 3T).
        var rand = SecureRandom()

        // Kotlin initializes the sampleBuffer with zeros
        var sampleBuffer = ShortArray(numSamples)
        var sampleBufferDouble = DoubleArray(numSamples)


        // Play the generated audio
        var audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, sampleBuffer.size * 2, AudioTrack.MODE_STREAM
        )
        audioTrack.play()
        var i = 1
        while (running) {
            // Initialize buffers with zeros
            sampleBuffer = ShortArray(numSamples)
            sampleBufferDouble = DoubleArray(numSamples)

            // Generate fresh random signal
            slots.forEachIndexed { slotIndex, slot ->
                if (slot == 1) {
                    for (sampleIndex in param.samplesPerSlot * slotIndex until param.samplesPerSlot * (slotIndex + 1)) {
                        sampleBufferDouble[sampleIndex] = (rand.nextGaussian() * TX_GAIN)
                    }
                }
            }
            // Set first and last samples to zero to avoid clipping between adjacent buffers
            val ZEROS_PAD: Int = (param.samplesPerSlot * WINDOWING_FACTOR).toInt()
            for (i in (0 until ZEROS_PAD) + ((numSamples - ZEROS_PAD) until numSamples)) {
                sampleBufferDouble[i] = 0.0;
            }

            info("Before filtering sampleBufferDouble max is ${sampleBufferDouble.max()}")

            // Remove frequencies outside the target band
            bandpass16(sampleBufferDouble)
            bandpass16(sampleBufferDouble)

            // Convert to short, which is expected by AICTransmitter
            sampleBufferDouble.forEachIndexed { index, sample ->
                sampleBuffer[index] = sample.toShort()
            }

            info("After filtering sampleBuffer max is ${sampleBuffer.max()}")
            info("sampleBuffer avg is ${sampleBuffer.average()}")

            info("Playing $i...")
            i++
            audioTrack.write(sampleBuffer, 0, sampleBuffer.size)
        }
        audioTrack.stop()
        audioTrack.release()
    }

    fun stop() {
        running = false
    }

}