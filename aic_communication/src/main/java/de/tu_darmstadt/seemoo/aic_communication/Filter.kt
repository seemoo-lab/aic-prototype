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
import be.tarsos.dsp.AudioProcessor
import org.jetbrains.anko.AnkoLogger

/**
 * Filter interface supporting Floats and Doubles.
 */
interface Filter {
    fun filter(samples: FloatArray)
    fun filter(samples: DoubleArray)
}

/**
 * Apply the given filters in order.
 */
fun applyFilters(samples: DoubleArray, filters: List<Filter>) {
    for (f in filters) {
        f.filter(samples)
    }
}

/**
 * Apply the given filters in order.
 */
fun applyFilters(samples: FloatArray, filters: List<Filter>) {
    for (f in filters) {
        f.filter(samples)
    }
}


/**
 * Bandpass filter of order 16.
 *
 * Passband: 16 kHz - 20 kHz @ samplerate 44.1 kHz
 */
fun bandpass16(samples: FloatArray) {
    applyFilters(samples, BIQUAD16)
}

/**
 * Bandpass filter of order 16.
 *
 * Passband: 16 kHz - 20 kHz @ samplerate 44.1 kHz
 */
fun bandpass16(samples: DoubleArray) {
    applyFilters(samples, BIQUAD16)
}

val BIQUAD16 = listOf(
    CustomBiQuadFilter(
        0.273770894534351,
        0.0,
        -0.273770894534351,
        1.286272160115626,
        0.972987565135578
    ),
    CustomBiQuadFilter(
        0.273770894534351,
        0.0,
        -0.273770894534351,
        1.905085595263964,
        0.989638593492713
    ),
    CustomBiQuadFilter(
        0.232023539931801,
        0.0,
        -0.232023539931801,
        1.338924728471972,
        0.926027252613301
    ),
    CustomBiQuadFilter(
        0.232023539931801,
        0.0,
        -0.232023539931801,
        1.869699781125356,
        0.967303218714224
    ),
    CustomBiQuadFilter(
        0.159525908611696,
        0.0,
        -0.159525908611696,
        1.454301303498322,
        0.896614395461604
    ),
    CustomBiQuadFilter(
        0.159525908611696,
        0.0,
        -0.159525908611696,
        1.810698783187075,
        0.940696790083472
    ),
    CustomBiQuadFilter(
        0.072672314933219,
        0.0,
        -0.072672314933219,
        1.593213787693579,
        0.892653028478772
    ),
    CustomBiQuadFilter(
        0.072672314933219,
        0.0,
        -0.072672314933219,
        1.718645688270213,
        0.911972825178633
    )
)


/**
 * Bandpass filter of order 8.
 *
 * Passband: 16 kHz - 20 kHz @ samplerate 44.1 kHz
 */
fun bandpass8(samples: DoubleArray) {
    applyFilters(samples, BIQUAD8)
}

val BIQUAD8 = listOf(
    CustomBiQuadFilter(
        0.261746541334431,
        0.0,
        -0.261746541334431,
        1.242445717043274,
        0.896583719538445
    ),
    CustomBiQuadFilter(
        0.261746541334431,
        0.0,
        -0.261746541334431,
        1.874853345955770,
        0.959006889634616
    ),
    CustomBiQuadFilter(
        0.136128609783833,
        0.0,
        -0.136128609783833,
        1.431983491098659,
        0.789203521489162
    ),
    CustomBiQuadFilter(
        0.136128609783833,
        0.0,
        -0.136128609783833,
        1.710429853606213,
        0.857599652965647
    )
)

/**
 * BiQuad filter with given coefficients.
 */
class CustomBiQuadFilter(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double
) : Filter, AnkoLogger {
    private var i1: Double = 0.0
    private var i2: Double = 0.0
    private var o1: Double = 0.0
    private var o2: Double = 0.0

    override fun filter(samples: DoubleArray) {
        i2 = 0.0
        i1 = 0.0
        o1 = 0.0
        o2 = 0.0
        for (j in samples.indices) {
            val i0 = samples[j]
            val o0: Double = b0 * i0 + b1 * i1 + b2 * i2 - a1 * o1 - a2 * o2
            samples[j] = o0
            i2 = i1
            i1 = i0
            o2 = o1
            o1 = o0
        }
    }

    override fun filter(samples: FloatArray) {
        i2 = 0.0
        i1 = 0.0
        o1 = 0.0
        o2 = 0.0
        for (j in samples.indices) {
            val i0 = samples[j].toDouble()
            val o0: Double = b0 * i0 + b1 * i1 + b2 * i2 - a1 * o1 - a2 * o2
            samples[j] = o0.toFloat()
            i2 = i1
            i1 = i0
            o2 = o1
            o1 = o0
        }
    }

}

/**
 * Pipeline bandpass filter realized using biquads.
 */
class BiQuadBP : AudioProcessor {
    override fun process(audioEvent: AudioEvent?): Boolean {
        val audioFloatBuffer = audioEvent!!.floatBuffer
        bandpass16(audioFloatBuffer)
        return true
    }

    override fun processingFinished() {
        //TODO("not implemented")
    }
}

/**
 * Pipeline bandpass filter, implemented as single higher order filter.
 */
class CustomBP : AudioProcessor {
    //protected var b: FloatArray = FloatArray(5) // Initialized to zeroes
    //protected var a: FloatArray = FloatArray(5) // Initialized to zeroes
    protected var b: FloatArray = FloatArray(1) // b(1), b(2), b(3), ...
    protected var a: FloatArray = FloatArray(1) // a(1), a(2), a(3), ...

    private var input: FloatArray = FloatArray(b.size) // x(i), x(i-1), x(i-2), ...
    private var output: FloatArray = FloatArray(a.size) // y(i-1), y(i-2), y(i-3), ...

    init {
        initIir6()
    }


    fun initFir20() {
        b = floatArrayOf(
            -0.0375456499352486f, 0.0292305056697396f, -0.0767904744855243f,
            0.0225357355914646f, 0.0187330566168225f, 0.0335343060634348f,
            -0.119046241096940f, 0.115344872050064f, 0.0370044608390968f,
            -0.252375277705109f, 0.354913056857792f, -0.252375277705109f,
            0.0370044608390968f, 0.115344872050064f, -0.119046241096940f,
            0.0335343060634348f, 0.0187330566168225f, 0.0225357355914646f,
            -0.0767904744855243f, 0.0292305056697396f, -0.0375456499352486f
        )
        a = floatArrayOf(1f)
        input = FloatArray(b.size)
        output = FloatArray(a.size)
    }

    fun initIir10() {
        b = floatArrayOf(
            0.000532491682523644f,
            0f,
            -0.00266245841261822f,
            0f,
            0.00532491682523644f,
            0f,
            -0.00532491682523644f,
            0f,
            0.00266245841261822f,
            0 - 0.000532491682523644f
        )
        a = floatArrayOf(
            1f,
            7.45473290708089f,
            26.3618625518104f,
            58.1201172776166f,
            88.3741020499795f,
            96.7693340706885f,
            77.2591491994603f,
            44.4212524277915f,
            17.6206139664228f,
            4.36284466328108f,
            0.514005902217825f
        )
        input = FloatArray(b.size)
        output = FloatArray(a.size)
    }

    fun initIir6() {
        b = floatArrayOf(
            0.0595360996815403f,
            0f,
            -0.119072199363081f,
            0f,
            0.0595360996815403f
        )
        a = floatArrayOf(
            1f,
            2.86328133566500f,
            3.43285219743198f,
            2.07011072868528f,
            0.545879378238323f
        )
        input = FloatArray(b.size)
        output = FloatArray(a.size)
    }


    override fun process(audioEvent: AudioEvent?): Boolean {
        val audioFloatBuffer = audioEvent?.floatBuffer

        for (i in audioEvent!!.getOverlap() until audioFloatBuffer!!.size) {
            //shift the in array
            System.arraycopy(input, 0, input, 1, input.size - 1) // TODO: inefficient
            input[0] = audioFloatBuffer[i]

            //calculate y based on a and b coefficients
            //and in and out.
            var y = 0f
            for (j in b.indices) {
                y += b[j] * input[j]
            }
            for (j in a.indices.drop(1)) { // skip a(1) since this is assumed to be normalized to 1
                y -= a[j] * output[j - 1]
            }
            //shift the out array
            System.arraycopy(output, 0, output, 1, output.size - 1) // TODO: inefficient
            output[0] = y

            audioFloatBuffer[i] = y
        }
        return true
    }


    override fun processingFinished() {
        //TODO("not implemented")
    }
}


