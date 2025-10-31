package com.example.smartwatchcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartwatchcompanion.ui.theme.SmartwatchCompanionTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartwatchcompanion.ui.theme.SmartwatchCompanionTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                0
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                0
            )
        }

        enableEdgeToEdge() // show top and bottom edges of screen
        setContent {
            SmartwatchCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),              // fill entire screen
                    color = MaterialTheme.colorScheme.background    // use theme background color
                ) {
                    SmartwatchDashboard()   // call composable for dashboard
                }
            }
        }
    }
}

@Composable
fun SmartwatchDashboard() {
    var heartRate by remember { mutableIntStateOf(72) }    // initial heart rate (fake) and updatable
    var steps by remember { mutableIntStateOf(3567) }      // initial step count (fake) and updatable
    var battery by remember { mutableIntStateOf(82) }      // initial battery level (fake) and updatable
    var wifiName by remember { mutableStateOf("Unknown") } // WIFI var
    var btStatus by remember { mutableStateOf("Unknown") } // BT var
    var accelData by remember { mutableStateOf("AcX = - | AcY = - | AcZ = -") }
    var gyroData by remember { mutableStateOf("GyX = - | GyY = - | GyZ = - | Temp = -") }
    val context = LocalContext.current // context reference

    Column(     // for vertical layout, elements are stacked vertically on screen
        modifier = Modifier
            .fillMaxSize()      // fill entire screen
            .padding(32.dp),    // add padding
        verticalArrangement = Arrangement.Center,   // vertically center elements
        horizontalAlignment = Alignment.CenterHorizontally  //  horizontally center elements
    ) {
        Text(text = "Smartwatch Dashboard", style = MaterialTheme.typography.headlineSmall) // display text and choose style
        Spacer(modifier = Modifier.height(24.dp)) // add vertical space between dashboard text and data text

        Text(text = "Heart Rate: $heartRate bpm", style = MaterialTheme.typography.bodyLarge) // display heart rate
        Spacer(modifier = Modifier.height(7.dp)) // add space

        Text(text = "Steps: $steps", style = MaterialTheme.typography.bodyLarge)  // display step count
        Spacer(modifier = Modifier.height(7.dp)) // add space

        Text(text = "Watch Battery: $battery%", style = MaterialTheme.typography.bodyLarge) // display battery percentage
        Spacer(modifier = Modifier.height(7.dp))   // add space

        Text(text = "Wi-Fi: $wifiName", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(7.dp))   // add space

        Text(text = "Bluetooth: $btStatus", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))   // add space between data display and refresh button

        Text("Acceleration Data:", style = MaterialTheme.typography.titleMedium)
        Text(accelData, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Gyroscope Data:", style = MaterialTheme.typography.titleMedium)
        Text(gyroData, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(96.dp))

        Button(onClick = {      // create button for updated random values when clicked
            heartRate = Random.nextInt(60, 101) // random HR between 60 - 100 bpm
            steps = Random.nextInt(0, 10001) // random # of steps between 0 - 10k
            battery = Random.nextInt(0, 101) // random battery percentage 0% - 100%

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager // get WIFI SSID
            @Suppress("DEPRECATION")       // suppress error for now
            val info = wifiManager.connectionInfo   // get WIFI connection information
            wifiName = info.ssid.removePrefix("\"").removeSuffix("\"") // assign WIFI name to var

            thread {
                try {
                    val bluetoothManager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
                    val adapter = bluetoothManager.adapter

                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // safe to use BluetoothAdapter and connect
                    } else {
                        btStatus = "Bluetooth permission not granted"
                    }

                    val deviceName = "ESP32_BT" // change this to match your ESP32's Bluetooth name
                    val device: BluetoothDevice? = adapter.bondedDevices.find { it.name == deviceName }

                    if (device != null) {
                        val uuid: UUID = device.uuids[0].uuid
                        val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                        adapter.cancelDiscovery()
                        socket.connect()

                        btStatus = "Connected"

                        val reader = BufferedReader(InputStreamReader(socket.inputStream))
                        val line = reader.readLine()

                        // Example input: AcX = 6852 | AcY = -13832 | AcZ = -5696 | GyX = -359 | GyY = -16 | GyZ = 162 | Temp = 44.20
                        val parts = line.split("|").map { it.trim() }

                        val accelParts = parts.filter { it.startsWith("Ac") }
                        val gyroParts = parts.filter { it.startsWith("Gy") || it.startsWith("Temp") }

                        accelData = accelParts.joinToString(" | ")
                        gyroData = gyroParts.joinToString(" | ")

                        socket.close()
                    } else {
                        btStatus = "ESP32 Not Found"
                    }
                } catch (e: Exception) {
                    btStatus = "Error: ${e.message}"
                }
            }

        }) {
            Text("Refresh Data") // text next to button
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SmartwatchDashboardPreview() {
    SmartwatchCompanionTheme { // apply app theme for preview
        SmartwatchDashboard() // display dashboard composable in preview mode
    }
}