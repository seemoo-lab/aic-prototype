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

package de.tu_darmstadt.seemoo.aic_prototype

import de.tu_darmstadt.seemoo.aic_communication.CALIBRATION_DATA
import de.tu_darmstadt.seemoo.aic_communication.Parameters

val DEFAULT_PARAMETERS = Parameters(44_100, 0.02f, 16_000, 20_000, -87, 20, 12)
val CALIBRATION_PARAMETERS =
    Parameters(44_100, 0.02f, 16_000, 20_000, -87, 20, CALIBRATION_DATA.size)
