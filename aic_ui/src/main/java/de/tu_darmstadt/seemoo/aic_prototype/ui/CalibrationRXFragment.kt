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
import android.view.View
import de.tu_darmstadt.seemoo.aic_communication.*
import de.tu_darmstadt.seemoo.aic_prototype.CALIBRATION_PARAMETERS

/**
 * RX Fragment for receiving pre-defined data for demo and calibration purposes.
 *
 */
class CalibrationRXFragment : RXFragment() {

    override var param = CALIBRATION_PARAMETERS

    override fun onAICReceived(
        mlBitstring: String,
        thresholdBitstring: String,
        slotInfo: ReceivedSlotInformation
    ) {
        tvOnPowerMin.text = "${slotInfo.onPowerMinimum()} dB"
        tvOffPowerMin.text = "${slotInfo.offPowerMinimum()} dB"
        tvOnPowerAvg.text = "${slotInfo.onPowerAverage()} dB"
        tvOffPowerAvg.text = "${slotInfo.offPowerAverage()} dB"
        tvOnPowerMax.text = "${slotInfo.onPowerMaximum()} dB"
        tvOffPowerMax.text = "${slotInfo.offPowerMaximum()} dB"
        tvTotalPower.text = "${slotInfo.totalPowerAverage()} dB"

        var calibrationBitstring = CALIBRATION_DATA.joinToString("")

        if (calibrationBitstring == mlBitstring) {
            tvDecoded.setTextColor(Color.GREEN)
        } else {
            tvDecoded.setTextColor(Color.RED)
        }
        var errorsML = countStringDifferences(calibrationBitstring, mlBitstring)
        tvDecoded.visibility = View.VISIBLE
        tvDecoded.text = "Errors: $errorsML/${thresholdBitstring.length}"


        if (calibrationBitstring == thresholdBitstring) {
            tvDecodedSecure.setTextColor(Color.GREEN)
        } else {
            tvDecodedSecure.setTextColor(Color.RED)
        }
        var errorsThreshold = countStringDifferences(calibrationBitstring, thresholdBitstring)
        var errorsThresholdUnclear = countStringUnclearBits(thresholdBitstring)
        tvDecodedSecure.visibility = View.VISIBLE
        tvDecodedSecure.text =
            "Errors: $errorsThreshold/${thresholdBitstring.length} Threshold: $errorsThresholdUnclear"
    }
}
