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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.tu_darmstadt.seemoo.aic_communication.AICTransmitter
import de.tu_darmstadt.seemoo.aic_prototype.DEFAULT_PARAMETERS
import de.tu_darmstadt.seemoo.aic_prototype.R
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * This fragment is responsible for setting up the basic TX UI.
 *
 * It can be extended for specific use cases.
 *
 */
open class TXFragment : Fragment(), AnkoLogger {
    private var tx: AICTransmitter? = null
    private var txThread: Thread? = null
    private lateinit var transmissionIndicator: TextView
    protected lateinit var dataInput: EditText

    // Default parameters
    protected open var param = DEFAULT_PARAMETERS

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_transmission, container, false)
        dataInput = view.findViewById(R.id.dataInput)
        transmissionIndicator = view.findViewById(R.id.tvTransmissionIndicator)
        transmissionIndicator.visibility = View.INVISIBLE

        view.findViewById<Button>(R.id.btnPlay)?.setOnClickListener { v ->
            startTX()
        }
        view.findViewById<Button>(R.id.btnStop)?.setOnClickListener { v ->
            stopTX()
        }

        return view
    }

    override fun onDetach() {
        super.onDetach()
        stopTX()
    }

    /**
     * Starts AIC transmission.
     */
    private fun startTX() {
        val data = getTXData() ?: return
        data.map { info("Data: $it") }

        tx = AICTransmitter(data, param)
        txThread = Thread(tx, "Audio Playback")
        txThread?.start()

        transmissionIndicator.visibility = View.VISIBLE
    }

    /**
     * Stops AIC transmission.
     */
    private fun stopTX() {
        info("Stop Modulation...")
        // stops the recording activity
        if (tx != null) {
            info("Stopping the audio generator.")
            tx?.stop()
            tx = null
            transmissionIndicator.visibility = View.INVISIBLE
        }
    }

    /**
     * Returns the bits that should be transmitted.
     */
    protected open fun getTXData(): IntArray? {
        // Read the TX data from the textbox.
        var dataIn = dataInput?.text ?: ""
        if (dataIn.length != param.symbolsPerFrame) {
            info("Aborting transmission: Data Input must be ${param.symbolsPerFrame}, not ${dataIn.length}")
            Toast.makeText(
                activity,
                "Data Input must be of size ${param.symbolsPerFrame}, not ${dataIn.length}",
                Toast.LENGTH_LONG
            ).show();
            return null
        }

        if (dataIn.contains(Regex("[^01]"))) {
            info("Aborting transmission: Data input can only contain 0 or 1.")
            Toast.makeText(
                activity, "Data input can only contain 0 or 1.",
                Toast.LENGTH_LONG
            ).show();
            return null
        }

        val data = dataIn.map { it.toString().toInt() }.toIntArray()
        return data
    }


}
