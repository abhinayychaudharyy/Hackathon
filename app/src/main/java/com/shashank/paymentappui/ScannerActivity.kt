package com.shashank.paymentappui



import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanOptions
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ScannerActivity : AppCompatActivity() {
    companion object {
        val paymentHistory = mutableListOf<PaymentRecord>()
    }

    private lateinit var scanLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register the launcher for ScanContract
        scanLauncher = registerForActivityResult(com.journeyapps.barcodescanner.ScanContract()) { result ->
            if (result != null && result.contents != null) {
                val record = PaymentRecord(result.contents, System.currentTimeMillis())
                paymentHistory.add(0, record)
                Toast.makeText(this, "Paid successfully!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, PaymentHistoryActivity::class.java))
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 123)
        } else {
            launchScanner()
        }
    }

    private fun launchScanner() {
        val options = ScanOptions()
        options.setPrompt("Scan QR code to pay")
        options.setBeepEnabled(true)
        options.setCaptureActivity(CaptureActivity::class.java)
        scanLauncher.launch(options)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchScanner()
        } else {
            Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}

data class PaymentRecord(val recipient: String, val timestamp: Long)

