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

package de.tu_darmstadt.seemoo.aic_prototype.ui

import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.axet.audiolibrary.widgets.PitchView
import de.tu_darmstadt.seemoo.aic_communication.AICReceiver
import de.tu_darmstadt.seemoo.aic_communication.Parameters
import de.tu_darmstadt.seemoo.aic_communication.ReceivedSlotInformation
import de.tu_darmstadt.seemoo.aic_communication.ReceptionHandler
import de.tu_darmstadt.seemoo.aic_prototype.DEFAULT_PARAMETERS
import de.tu_darmstadt.seemoo.aic_prototype.R
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import java.io.File


/**
 * This fragment is responsible for setting up the basic TX UI.
 *
 * It can be extended for specific use cases.
 *
 */
open class RXFragment : Fragment(), AnkoLogger {
    private var rx: AICReceiver? = null

    protected lateinit var pitchView: PitchView
    protected lateinit var pitchViewFiltered: PitchView
    protected lateinit var tvDetector: TextView
    protected lateinit var tvDecoded: TextView
    protected lateinit var tvDecodedSecure: TextView
    protected lateinit var tvOnPowerMin: TextView
    protected lateinit var tvOffPowerMin: TextView
    protected lateinit var tvOnPowerAvg: TextView
    protected lateinit var tvOffPowerAvg: TextView
    protected lateinit var tvOnPowerMax: TextView
    protected lateinit var tvOffPowerMax: TextView
    protected lateinit var tvTotalPower: TextView

    protected open var param = DEFAULT_PARAMETERS


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_reception, container, false)
        pitchView = view.findViewById(R.id.chart)
        pitchViewFiltered = view.findViewById((R.id.chartFiltered))
        tvDetector = view.findViewById(R.id.detector)
        tvDecoded = view.findViewById(R.id.tvDecoded)
        tvDecoded.visibility = View.INVISIBLE
        tvDecodedSecure = view.findViewById(R.id.tvDecodedSecure)
        tvDecodedSecure.visibility = View.INVISIBLE
        tvOnPowerMin = view.findViewById(R.id.tvOnPowerMin)
        tvOffPowerMin = view.findViewById(R.id.tvOffPowerMin)
        tvOnPowerAvg = view.findViewById(R.id.tvOnPowerAvg)
        tvOffPowerAvg = view.findViewById(R.id.tvOffPowerAvg)
        tvOnPowerMax = view.findViewById(R.id.tvOnPowerMax)
        tvOffPowerMax = view.findViewById(R.id.tvOffPowerMax)
        tvTotalPower = view.findViewById(R.id.tvTotalPower)

        view.findViewById<Button>(R.id.btnStart).setOnClickListener { view ->
            debug("btnStart on click listener")
            startRX()
        }

        view.findViewById<Button>(R.id.btnStop).setOnClickListener { view ->
            stopRX()
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        // Volume controls now control music stream
        activity?.volumeControlStream = AudioManager.STREAM_MUSIC
    }

    /**
     * Starts AIC reception.
     */
    private fun startRX() {
        debug("Start decoding")

        pitchView.setOnTouchListener(null)
        pitchView.record()

        pitchViewFiltered.setOnTouchListener(null)
        pitchViewFiltered.record()

        // Set up output dir for AIC recordings.
        val path = activity?.applicationContext?.getExternalFilesDir(null)
        val outputDir = File(path, "aic-recordings")

        rx = AICReceiver(param, object : ReceptionHandler {
            override val recordingsDir: File = outputDir
            override fun stopRec() {
                activity?.runOnUiThread {
                    stopRX()
                }
            }

            override fun handleDecoded(
                mlBitstring: String,
                thresholdBitstring: String,
                slotInfo: ReceivedSlotInformation
            ) {
                activity?.runOnUiThread {
                    this@RXFragment.onAICReceived(mlBitstring, thresholdBitstring, slotInfo)
                }
            }

            override fun handleThresholdSurpassed(db: Double) {
                activity?.runOnUiThread {
                    // NOTE: These values for the visual indicator are probably device specific.
                    // These are approximate values for the OnePlus 3T.
                    if (db >= param.thresholdHigh + 8) {
                        tvDetector.text = "Too loud"
                        tvDetector.setTextColor(Color.YELLOW)
                    } else if (db >= param.thresholdHigh - 2) {
                        tvDetector.text = "Good"
                        tvDetector.setTextColor(Color.GREEN)
                    } else {
                        tvDetector.text = "Too quiet"
                        tvDetector.setTextColor(Color.RED)
                    }
                }
            }

            override fun filteredBufferFilled(db: Double) {
                activity?.runOnUiThread {
                    pitchViewFiltered.add(db + 10)
                }
            }

            override fun rawBufferFilled(db: Double) {
                activity?.runOnUiThread {
                    pitchView.add(db)
                }
            }
        })

        rx?.start()
        debug("Starting recording done")
    }

    /**
     * Stops AIC reception.
     */
    private fun stopRX() {
        if (rx != null) {
            debug("Stopping the recording dispatcher.")
            rx?.stop()
            rx = null
        }

        pitchView.stop()
        pitchViewFiltered.stop()
    }

    /**
     * Callback function that updates the UI once an AIC signal was successfully received and decoded.
     */
    protected open fun onAICReceived(
        mlBitstring: String,
        thresholdBitstring: String,
        slotInfo: ReceivedSlotInformation
    ) {
        tvDecoded.visibility = View.VISIBLE
        tvDecoded.text = mlBitstring

        tvDecodedSecure.visibility = View.VISIBLE
        tvDecodedSecure.text = thresholdBitstring

        tvOnPowerMin.text = "${slotInfo.onPowerMinimum()} dB"
        tvOffPowerMin.text = "${slotInfo.offPowerMinimum()} dB"
        tvOnPowerAvg.text = "${slotInfo.onPowerAverage()} dB"
        tvOffPowerAvg.text = "${slotInfo.offPowerAverage()} dB"
        tvOnPowerMax.text = "${slotInfo.onPowerMaximum()} dB"
        tvOffPowerMax.text = "${slotInfo.offPowerMaximum()} dB"
        tvTotalPower.text = "${slotInfo.totalPowerAverage()} dB"
    }


}
