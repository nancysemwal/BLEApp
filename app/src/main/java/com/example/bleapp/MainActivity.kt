package com.example.bleapp

import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bleapp.ui.theme.BLEAppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import java.util.regex.Pattern

@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLEAppTheme {
                PairingScreen(companionDeviceManager = deviceManager)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PairingScreen(
    companionDeviceManager: CompanionDeviceManager
) {
    val (pairingStatus, setPairingStatus) = remember {
        val initialState = if (companionDeviceManager.associations
                .isNotEmpty()
        ) {
            PairingStatus.Paired
        } else {
            PairingStatus.NotPaired
        }
        mutableStateOf(initialState)
        //mutableStateOf(PairingStatus.NotPaired)
    }
    val (deviceAddress, setDeviceAddress) = remember {
        val initialState = companionDeviceManager.associations
            .firstOrNull() ?: "unknown"
        mutableStateOf(initialState)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Device pairing",
            style = MaterialTheme.typography.h2
        )

        Text(
            "Pairing status: ${pairingStatus.name}",
            style = MaterialTheme.typography.subtitle1
        )

        Text(
            "Device address: $deviceAddress",
            style = MaterialTheme.typography.subtitle2
        )

        val btnEnabled = when (pairingStatus) {
            PairingStatus.Paired, PairingStatus.NotPaired -> true
            else -> false
        }
        val contract = ActivityResultContracts.StartIntentSenderForResult()
        val activityResultLauncher =
            rememberLauncherForActivityResult(contract = contract) {
                it.data
                    ?.getParcelableExtra<ScanResult>(CompanionDeviceManager.EXTRA_DEVICE)
                    ?.let { scanResult ->
                        val device = scanResult.toString()
                        setPairingStatus(PairingStatus.Paired)
                        setDeviceAddress(device)
                        //val device = scanResult.device
                        //setDeviceAddress(device.address)
                    }
            }

        Button(onClick = {
            if(pairingStatus == PairingStatus.NotPaired) {
                val associationRequest : AssociationRequest = buildAssociationRequest();
                companionDeviceManager.associate(
                    associationRequest,
                    object : CompanionDeviceManager.Callback(){
                        override fun onDeviceFound(chooserLauncher: IntentSender?) {
                            chooserLauncher?.let {
                                val request = IntentSenderRequest.Builder(it).build()
                                activityResultLauncher.launch(request)
                            }
                        }

                        override fun onFailure(error: CharSequence?) {
                            setPairingStatus(PairingStatus.PairingFailed)
                        }
                    }, null)
            } else if (pairingStatus == PairingStatus.Paired) {
                companionDeviceManager.disassociate(deviceAddress)
            }
        }
        ) {
            val label = when (pairingStatus) {
                PairingStatus.Paired -> "Forget device"
                PairingStatus.NotPaired -> "Start pairing"
                PairingStatus.PairingFailed -> "Error!"
                PairingStatus.Pairing -> "Pairing..."
            }
            Text(label)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun buildAssociationRequest(): AssociationRequest {
    val deviceFilter = BluetoothLeDeviceFilter.Builder()
        .setNamePattern(Pattern.compile("Bhajanollas"))
        .build()
    return AssociationRequest.Builder()
        .addDeviceFilter(deviceFilter)
        .build()
}

enum class PairingStatus {
    NotPaired, Pairing, Paired, PairingFailed
}

