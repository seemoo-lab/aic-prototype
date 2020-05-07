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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.jakewharton.threetenabp.AndroidThreeTen
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug


private const val REQUEST_RECORD_AUDIO_PERMISSION = 200


/**
 * Main activity which handles permissions and sets up the navigation.
 *
 */
class MainActivity : AppCompatActivity(), AnkoLogger {

    val drawerLayout by lazy { findViewById<DrawerLayout>(R.id.activity_main) }
    val navController by lazy { findNavController(R.id.nav_host_fragment) }
    val navigationView by lazy { findViewById<NavigationView>(R.id.nav_view) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this, permissions,
            REQUEST_RECORD_AUDIO_PERMISSION
        )

        // Handle navigation item clicks on the drawer
        navigationView.setupWithNavController(navController)
    }

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) {
            // TODO: Error Handling
            error("PERMISSION NOT ACCEPTED! THIS IS CURRENTLY UNHANDLED.")
        }
    }

    override fun onResume() {
        super.onResume()
        debug("onResume")

    }

    override fun onPause() {
        super.onPause()
        debug("onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        debug("onDestroy")
        //stopRecording()
    }
}