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
import android.media.AudioRecord
import android.media.MediaRecorder
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AndroidAudioInputStream
import be.tarsos.dsp.writer.WriterProcessor
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.info
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.*


/**
 * Callback handler for demodulating AICs.
 */
interface DemodulatedAICHandler {
    fun handleThresholdSurpassed(info: Double)
    fun stopRec()

    //fun exportBuffer(frameBuffer: FloatArray)
    fun handleDecoded(
        mlBitstring: String,
        thresholdBitstring: String,
        slotInfo: ReceivedSlotInformation
    )
}

/**
 * Callback handler for receiving AICs.
 *
 * (Also includes callbacks for demodulating AICs.)
 */
interface ReceptionHandler : DemodulatedAICHandler {
    val recordingsDir: File
    fun rawBufferFilled(db: Double)
    fun filteredBufferFilled(db: Double)
}

/**
 * Receives an AIC signal using the microphone.
 *
 * @property handler the callback handler to call when AIC is received
 */
class AICReceiver(private var param: Parameters, private var handler: ReceptionHandler) :
    AnkoLogger {

    private var dispatcher: AudioDispatcher? = null

    /**
     * Starts AIC reception. The result gets communicated back using the [handler].
     *
     * Stores recordings in internal storage.
     */
    fun start() {
        dispatcher = getAICMicrophoneAudioDispatcher()

        // Print statistical information about recorded samples
        dispatcher?.addAudioProcessor(CalcPowerProcessor(object : CalcPowerHandler {
            override fun handleInfo(info: Double) {
                handler.rawBufferFilled(info)
            }
        }, "RAW"))

        // Export the raw samples as wavefile
        handler.recordingsDir.mkdirs()
        val timestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .format(LocalDateTime.now())
        val outputFileRaw = File(handler.recordingsDir, "$timestamp-raw.wav")
        info("Output file name (raw): ${outputFileRaw.absolutePath}")
        val outputRAFileRaw = RandomAccessFile(outputFileRaw, "rw")
        dispatcher?.addAudioProcessor(WriterProcessor(AUDIO_FORMAT, outputRAFileRaw))


        // Reception filter (bandpass)
        dispatcher?.addAudioProcessor(CustomBP())
        // Alternative bandpass implementations
        // val bandwith = (F_HIGH - F_LOW).toFloat()
        // val freq_center = F_LOW + bandwith/2.0f
        // dispatcher?.addAudioProcessor(BandPass(freq_center, bandwith, RECORDER_SAMPLERATE.toFloat()))
        // dispatcher?.addAudioProcessor(BiQuadBP())

        // Print statistical information about recorded samples after filtering
        dispatcher?.addAudioProcessor(CalcPowerProcessor(object : CalcPowerHandler {
            override fun handleInfo(db: Double) {
                handler.filteredBufferFilled(db)
            }
        }, "BP filtered"))

        // Clock recovery + demodulation
        dispatcher?.addAudioProcessor(DemodulateAICProcessor(param, handler))

        // Export filtered recording as wavefile
        val outputFile = File(handler.recordingsDir, "$timestamp-filtered.wav")
        info("Output file name: ${outputFile.absolutePath}")
        val outputRAFile = RandomAccessFile(outputFile, "rw")
        dispatcher?.addAudioProcessor(WriterProcessor(AUDIO_FORMAT, outputRAFile))


        val audioThread = Thread(dispatcher, "Audio Dispatcher")
        audioThread?.start()
    }

    fun stop() {
        dispatcher?.stop()
    }

}


/**
 * Detects AICs in the message stream.
 *
 * @property handler the callback handler to call when an AIC is detected
 */
class DemodulateAICProcessor(
    private val param: Parameters,
    private val handler: DemodulatedAICHandler
) : AudioProcessor,
    AnkoLogger {
    var count: Int = 0
    var frameBuffer: FloatArray = FloatArray(param.bufferSize) // TODO: Too big?
    var fbPos = 0 // Position in frame buffer up to which it is filled
    var full = false

    override fun processingFinished() {
        // CLeanup
        frameBuffer = FloatArray(param.bufferSize) // TODO: Too big?

    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent?.floatBuffer

        if (full) {
            handler.stopRec()
            return false
        }
        // Fill the frame buffer
        // Check if the frame buffer has enough empty space
        if (fbPos + audioFloatBuffer.size > frameBuffer.size) {
            info("Buffer full. Starting processing")
            full = true
            // Here we can do the final processing
            // 1) Coarse synchronization to find the delimiter
            var thresholdBuffer =
                frameBuffer.map { sample: Float -> if (abs(sample) > THRESHOLD_SAMPLE) 1f else -1f }
                    .toFloatArray()

            // TODO: Could be extracted as constant?
            val referenceDelimiter = FloatArray(param.samplesPerDelimiter)
            param.delimiter.forEachIndexed { idx, delimiterBit ->
                referenceDelimiter.fill(
                    if (delimiterBit == 1) 1f else -1f,
                    idx * param.samplesPerSlot,
                    (idx + 1) * param.samplesPerSlot - 1
                )
            }

            // Cross-correlation in time domain
            var currentMax = 0f
            var maxDelCorrelationIdx = 0
            var current = 0f
            // TODO: Performance could be improved.
            for (i in 0 until (frameBuffer.size - param.samplesPerDelimiter - param.samplesPerFrame)) {
                current =
                    referenceDelimiter.foldIndexed(0f, { index: Int, acc: Float, sample: Float ->
                        acc + sample * thresholdBuffer[i + index]
                    })
                if (current > currentMax) {
                    currentMax = current
                    maxDelCorrelationIdx = i
                }
            }
            info("Delimiter correlation max: $currentMax")
            info("Delimiter correlation max index is $maxDelCorrelationIdx/${frameBuffer.size}, which is at ${100 * maxDelCorrelationIdx / frameBuffer.size}%")
            val delimiterEnd = maxDelCorrelationIdx + param.samplesPerDelimiter

            val checkFirst = delimiterEnd - param.samplesPerSlot / 2
            val checkLast = delimiterEnd + param.samplesPerSlot / 2
            info("Delimiter end is $delimiterEnd. Check in area around it: [$checkFirst, $checkLast].")


            // 2) Fine synchronization for precise delimiter location
            val referenceSymbol = FloatArray(param.samplesPerSymbol)
            referenceSymbol.fill(1f, 0, param.samplesPerSlot - 1)
            referenceSymbol.fill(-1f, param.samplesPerSlot, param.samplesPerSymbol)

            // CrossCorrelation with the reference symbol
            var refSymCorr = FloatArray(frameBuffer.size - param.samplesPerSymbol)
            // TODO: This is currently very slow.
            for (i in refSymCorr.indices) {
                refSymCorr[i] =
                    abs(referenceSymbol.foldIndexed(0f, { index: Int, acc: Float, sample: Float ->
                        acc + sample * thresholdBuffer[i + index]
                    }))
            }
            debug("refSymCorr done.")
            // Sample the cross-correlation with Ts and get the fine delay that results in the maximum sum
            currentMax = 0f
            var maxSymCorrelationIdx = 0
            for (delay in checkFirst..checkLast) {
                current = 0f
                for (i in delay until (delay + param.samplesPerFrame) step param.samplesPerSymbol) {
                    current += refSymCorr[i]
                }

                if (current > currentMax) {
                    currentMax = current
                    maxSymCorrelationIdx = delay
                }
            }

            val delay = maxSymCorrelationIdx
            info("Fine delay is $delay.")

            // Demodulate the symbols
            var demodulatedSymbolsML = IntArray(param.symbolsPerFrame)
            var demodulatedSymbolsSecure = IntArray(param.symbolsPerFrame)
            var ONPowers = DoubleArray(param.symbolsPerFrame)
            var OFFPowers = DoubleArray(param.symbolsPerFrame)
            var skipSlotSamplesBefore = ceil(0.15 * param.samplesPerSlot).toInt()
            var skipSlotSamplesAfter = floor(0.05 * param.samplesPerSlot).toInt()
            for (i in demodulatedSymbolsSecure.indices) {
                val startSample = delay + i * param.samplesPerSymbol
                val firstSamples =
                    frameBuffer.slice((startSample + skipSlotSamplesBefore) until (startSample + param.samplesPerSlot - skipSlotSamplesAfter))
                val firstPower = sqrt(firstSamples.map { sample -> sample * sample }.average())
                val firstPowerDb = 20f * log10(firstPower)

                val secondSamples =
                    frameBuffer.slice((startSample + param.samplesPerSlot + skipSlotSamplesBefore) until (startSample + param.samplesPerSymbol - skipSlotSamplesAfter))
                val secondPower = sqrt(secondSamples.map { sample -> sample * sample }.average())
                val secondPowerDb = 20f * log10(secondPower)

                // ML Estimate (insecure)
                demodulatedSymbolsML[i] = if (firstPowerDb > secondPowerDb) 1 else 0
                info("ML estimate $i: ${demodulatedSymbolsML[i]} ($firstPower [$firstPowerDb dB], $secondPower [$secondPowerDb dB])")

                if (firstPower > secondPower) {
                    ONPowers[i] = firstPower
                    OFFPowers[i] = secondPower
                } else {
                    ONPowers[i] = secondPower
                    OFFPowers[i] = firstPower
                }

                // Threshold estimate (secure)
                if (firstPowerDb >= param.thresholdHigh && secondPowerDb <= param.thresholdLow) {
                    demodulatedSymbolsSecure[i] = 1
                } else if (firstPowerDb <= param.thresholdLow && secondPowerDb >= param.thresholdHigh) {
                    demodulatedSymbolsSecure[i] = 0
                } else {
                    demodulatedSymbolsSecure[i] = 2
                }
                info("Secure estimate $i: ${demodulatedSymbolsSecure[i]} ($firstPower [$firstPowerDb dB], $secondPower [$secondPowerDb dB])")


            }

            val slotInfo = ReceivedSlotInformation(ONPowers, OFFPowers)

            val avgOnPower = slotInfo.onPowerAverage()
            val avgOffPower = slotInfo.offPowerAverage()
            val avgTotalPower = slotInfo.totalPowerAverage()
            info("Average of ON RMS powers is $avgOnPower dB. [Min= ${slotInfo.onPowerMinimum()}; Max= ${slotInfo.onPowerMaximum()}]")
            info("Average of OFF RMS powers is $avgOffPower dB. [Min= ${slotInfo.offPowerMinimum()}; Max= ${slotInfo.offPowerMaximum()}]")

            handler.handleDecoded(
                demodulatedSymbolsML.joinToString(""),
                demodulatedSymbolsSecure.joinToString(""),
                slotInfo
            )
        } else {
            System.arraycopy(audioFloatBuffer, 0, frameBuffer, fbPos, audioFloatBuffer.size)
            fbPos += audioFloatBuffer.size
        }

        //val rms = sqrt(audioFloatBuffer.map { sample: Float -> sample*sample }.sum() / audioFloatBuffer.size);
        val rms = audioEvent.rms
        val db = 20f * log10(rms)

        if (db >= param.thresholdHigh) {
            count++
            info("THRESHOLD surpassed $count (with $db dB)")
            handler.handleThresholdSurpassed(db)
        } else {
            handler.handleThresholdSurpassed(db)
        }

        return true
    }
}

/**
 * Callback handler for processing the recorded power.
 */
interface CalcPowerHandler {
    fun handleInfo(info: Double)
}

/**
 * Pipeline processor that calculates the power of recorded samples.
 */
class CalcPowerProcessor(private val handler: CalcPowerHandler, private val audioContext: String) :
    AudioProcessor, AnkoLogger {
    //private val handler: CalcPowerHandler

    override fun processingFinished() {
        return
    }

    override fun process(audioEvent: AudioEvent): Boolean {
        val audioFloatBuffer = audioEvent.getFloatBuffer()

        //val rms = sqrt(audioFloatBuffer.map { sample: Float -> sample*sample }.sum() / audioFloatBuffer.size);
        val rms = audioEvent.rms
        val db = 20f * log10(rms)

        info("$audioContext: Average of the last $RECORDER_BUFFER_SIZE samples: $rms ($db dB)")

        handler.handleInfo(db)
        return true
    }
}

/**
 * Pipeline audio dispatcher for accessing the microphone.
 */
fun getDefaultMicrophoneAudioDispatcher(
    sampleRate: Int,
    audioBufferSize: Int,
    bufferOverlap: Int
): AudioDispatcher {
    val minAudioBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        android.media.AudioFormat.CHANNEL_IN_MONO,
        android.media.AudioFormat.ENCODING_PCM_16BIT
    )
    val minAudioBufferSizeInSamples = minAudioBufferSize / 2
    if (minAudioBufferSizeInSamples <= audioBufferSize) {
        val audioInputStream = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED, sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            audioBufferSize * 2
        )

        val audioStream = AndroidAudioInputStream(audioInputStream, AUDIO_FORMAT)
        // Start recording. Opens the stream.
        audioInputStream.startRecording()
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    } else {
        throw IllegalArgumentException("Buffer size too small should be at least " + minAudioBufferSize * 2)
    }
}

/**
 * Pipeline audio dispatcher with default values for AIC recording.
 */
fun getAICMicrophoneAudioDispatcher(): AudioDispatcher {
    val dispatcher = getDefaultMicrophoneAudioDispatcher(
        RECORDER_SAMPLERATE,
        RECORDER_BUFFER_SIZE,
        0
    )
    return dispatcher
}