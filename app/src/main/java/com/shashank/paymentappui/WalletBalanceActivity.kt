package com.shashank.paymentappui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import android.app.AlertDialog

class WalletBalanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet_balance)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val balanceText = findViewById<TextView>(R.id.balanceAmount)
        val addMoneyButton = findViewById<Button>(R.id.addMoneyButton)
        var balance = 10000.0

        addMoneyButton.setOnClickListener {
            val input = EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = "Enter amount"
            AlertDialog.Builder(this)
                .setTitle("Add Money")
                .setView(input)
                .setPositiveButton("Add") { dialog, _ ->
                    val amountStr = input.text.toString()
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        balance += amount
                        balanceText.text = "₹%.2f".format(balance)
                        android.widget.Toast.makeText(this, "₹%.2f added to wallet!".format(amount), android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(this, "Enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .show()
        }
    }
}