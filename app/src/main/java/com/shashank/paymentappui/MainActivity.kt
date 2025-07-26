package com.shashank.paymentappui


import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.view.View
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.ImageButton
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.graphics.Bitmap
import android.speech.RecognizerIntent

data class Receipt(val photoUri: Uri, val extractedText: String, val total: Double, val date: String)
data class Transaction(val shopName: String, val amount: Double, val date: String)

class MainActivity : AppCompatActivity() {
    private val receiptsList = mutableListOf<Receipt>()
    private lateinit var receiptsRecyclerView: RecyclerView
    private lateinit var receiptsAdapter: ReceiptsAdapter
    private var REQUEST_CODE_CAMERA = 2001
    private var cameraImageUri: Uri? = null
    private val REQUEST_CODE_SPEECH = 1001
    private val REQUEST_CODE_MIC_PERMISSION = 1002
    private val REQUEST_CODE_GALLERY = 3001
    
    // Add back Recent Passes functionality
    private val defaultTransactions = listOf(
        Transaction("Big Market", 5250.0, "07 Jul 2024"),
        Transaction("Amazone", 2000.0, "06 Jul 2024"),
        Transaction("Swizzy", 3000.0, "05 Jul 2024")
    )
    private val transactionList = mutableListOf<Transaction>().apply { addAll(defaultTransactions) }
    private var defaultsRemoved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_file)

        val searchEdit = findViewById<EditText>(R.id.searchEdit)
        // Remove aiAnswerCard and aiAnswerText if not used
        // val aiAnswerCard = findViewById<LinearLayout>(R.id.aiAnswerCard)
        // val aiAnswerText = findViewById<TextView>(R.id.aiAnswerText)

        // Remove ListView setup
        // resultListView = ListView(this)
        // val layout = findViewById<LinearLayout>(R.id.rootLayout)
        // layout.addView(resultListView)
        // resultAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, chatList.map { "${'$'}{it.sender}: ${'$'}{it.text}" })
        // resultListView.adapter = resultAdapter

        // Initialize receipts RecyclerView
        receiptsRecyclerView = findViewById(R.id.receiptsRecyclerView)
        receiptsRecyclerView.layoutManager = LinearLayoutManager(this)
        receiptsAdapter = ReceiptsAdapter(receiptsList, 
            onItemClick = { receipt -> showReceiptDetailsDialog(receipt) },
            onDeleteClick = { receipt -> deleteReceipt(receipt) }
        )
        receiptsRecyclerView.adapter = receiptsAdapter

        searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing on text change (search only on Enter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Trigger search on Enter key (IME_ACTION_SEARCH or IME_ACTION_DONE)
        searchEdit.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = searchEdit.text.toString()
                if (query.isNotBlank()) {
                    // searchLovable(query) // Removed AI search
                }
                true
            } else {
                false
            }
        }

        searchEdit.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                val query = searchEdit.text.toString()
                if (query.isNotBlank()) {
                    // searchLovable(query) // Removed AI search
                }
                true
            } else {
                false
            }
        }

        // After your findViewById lines:
        val walletButton = findViewById<LinearLayout>(R.id.walletButton)
        val payButton = findViewById<LinearLayout>(R.id.payButton)
        val scannerButton = findViewById<LinearLayout>(R.id.scannerButton)
        val saveMoneyButton = findViewById<LinearLayout>(R.id.saveMoneyButton)
        val micButton = findViewById<ImageButton>(R.id.micButton)
        val cameraButton = findViewById<ImageButton>(R.id.cameraButton)

// Assign click listeners to open new activities:
        walletButton.setOnClickListener {
            startActivity(Intent(this, WalletBalanceActivity::class.java))
        }

        payButton.setOnClickListener {
            startActivity(Intent(this, PayActivity::class.java))
        }

        scannerButton.setOnClickListener {
            // Launches ScannerActivity, which opens the real device camera to scan a QR code
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        saveMoneyButton.setOnClickListener {
            startActivity(Intent(this, SaveMoneyActivity::class.java))
        }

        micButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_CODE_MIC_PERMISSION)
            } else {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH)
                } catch (e: Exception) {
                    Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraButton.setOnClickListener {
            val options = arrayOf("Take Photo", "Choose from Gallery")
            AlertDialog.Builder(this)
                .setTitle("Add Receipt")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // Take Photo
                            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_CAMERA)
                            } else {
                                openCameraForBill()
                            }
                        }
                        1 -> { // Choose from Gallery
                            val intent = Intent(Intent.ACTION_PICK)
                            intent.type = "image/*"
                            startActivityForResult(intent, REQUEST_CODE_GALLERY)
                        }
                    }
                }
                .show()
        }

        // Set up mini camera click listeners in Recent Passes section
        setupMiniCamListeners()
    }

    private fun openCameraForBill() {
        Toast.makeText(this, "Please take a clear, well-lit photo of the bill/receipt for best results.", Toast.LENGTH_LONG).show()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoUri = createImageUri()
        cameraImageUri = photoUri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        try {
            startActivityForResult(intent, REQUEST_CODE_CAMERA)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageUri(): Uri? {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "bill_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.getOrNull(0) ?: ""
            val searchEdit = findViewById<EditText>(R.id.searchEdit)
            searchEdit.setText(spokenText)
        } else if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                runTextRecognition(bitmap)
            }
        } else if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                cameraImageUri = imageUri // Save for receipts
                runTextRecognition(bitmap)
            }
        }
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                // Always add to receipts list
                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                cameraImageUri?.let { uri ->
                    receiptsList.add(0, Receipt(uri, text, 0.0, date))
                    receiptsAdapter.notifyItemInserted(0)
                }
                // Show dialog to let user review/edit recognized text before parsing
                showRecognizedTextDialog(text)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not extract text from image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRecognizedTextDialog(recognizedText: String) {
        val textView = TextView(this)
        textView.text = recognizedText
        textView.setPadding(32, 32, 32, 32)
        AlertDialog.Builder(this)
            .setTitle("Recognized Text")
            .setView(textView)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun parseShopAndAmountImproved(text: String): Pair<String, Double> {
        // Try to find amount using keywords like 'Total', 'Amount', etc.
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var shop = ""
        var amount = 0.0
        val amountKeywords = listOf("total", "amount", "grand total", "balance due")
        for (line in lines) {
            if (shop.isEmpty() && line.any { it.isLetter() }) {
                shop = line.take(32)
            }
            val lower = line.lowercase()
            if (amount == 0.0 && amountKeywords.any { lower.contains(it) }) {
                val amtMatch = Regex("[₹$]?([0-9]+[.,][0-9]{2,})").find(line)
                if (amtMatch != null) {
                    val amtStr = amtMatch.groupValues[1].replace(",", ".")
                    amount = amtStr.toDoubleOrNull() ?: 0.0
                }
            }
        }
        // Fallback: first number with decimal if not found by keyword
        if (amount == 0.0) {
            for (line in lines) {
                val amtMatch = Regex("[₹$]?([0-9]+[.,][0-9]{2,})").find(line)
                if (amtMatch != null) {
                    val amtStr = amtMatch.groupValues[1].replace(",", ".")
                    amount = amtStr.toDoubleOrNull() ?: 0.0
                    break
                }
            }
        }
        return Pair(shop, amount)
    }

    private fun setupMiniCamListeners() {
        // Find all mini camera buttons in the recent passes section and set click listeners
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        fun setMiniCamListener(view: View) {
            if (view is android.widget.ImageButton && view.contentDescription == "Pass Camera") {
                view.setOnClickListener { showAddTransactionDialog() }
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) setMiniCamListener(view.getChildAt(i))
            }
        }
        setMiniCamListener(rootLayout)
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val shopNameInput = dialogView.findViewById<EditText>(R.id.shopNameInput)
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        AlertDialog.Builder(this)
            .setTitle("Add Recent Pass")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val shop = shopNameInput.text.toString()
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                if (shop.isNotBlank() && amount > 0) {
                    if (!defaultsRemoved) {
                        transactionList.clear()
                        defaultsRemoved = true
                    }
                    transactionList.add(Transaction(shop, amount, date))
                    updateRecentPassesUI()
                } else {
                    Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRecentPassesUI() {
        // Find the recent passes LinearLayout and update its children
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        val recentPassesList = findRecentPassesList(rootLayout)
        recentPassesList?.let { listLayout ->
            listLayout.removeAllViews()
            for ((index, txn) in transactionList.withIndex()) {
                val row = LayoutInflater.from(this).inflate(R.layout.recent_pass_row, listLayout, false)
                val iconView = row.findViewById<ImageView>(R.id.shopIcon)
                when (txn.shopName.lowercase()) {
                    "big market" -> iconView.setImageResource(R.drawable.ic_market)
                    "amazone" -> iconView.setImageResource(R.drawable.ic_amazon)
                    "swizzy" -> iconView.setImageResource(R.drawable.ic_swizzy)
                    else -> iconView.setImageResource(R.drawable.ic_market)
                }
                row.findViewById<TextView>(R.id.shopName).text = txn.shopName
                row.findViewById<TextView>(R.id.amount).text = "₹%.2f".format(txn.amount)
                row.findViewById<TextView>(R.id.date).text = txn.date
                row.findViewById<ImageButton>(R.id.miniCamButton).setOnClickListener { showAddTransactionDialog() }
                val deleteButton = row.findViewById<ImageButton>(R.id.deleteButton)
                // Always show mini camera button
                // Only hide delete button for default transactions
                if (index < defaultTransactions.size && !defaultsRemoved) {
                    deleteButton.visibility = View.GONE
                } else {
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener {
                        transactionList.removeAt(index)
                        updateRecentPassesUI()
                    }
                }
                listLayout.addView(row)
            }
        }
        // Update total amount
        val totalAmountView = findViewById<TextView>(R.id.totalAmount)
        val total = transactionList.sumOf { it.amount }
        totalAmountView.text = "₹%.2f".format(total)
    }

    private fun findRecentPassesList(root: ViewGroup): LinearLayout? {
        // Find the LinearLayout that holds the recent passes rows
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL && child.childCount > 0) {
                // Heuristic: check if it contains a row with a Pass Camera button
                for (j in 0 until child.childCount) {
                    val row = child.getChildAt(j)
                    if (row is LinearLayout) {
                        for (k in 0 until row.childCount) {
                            val v = row.getChildAt(k)
                            if (v is android.widget.ImageButton && v.contentDescription == "Pass Camera") {
                                return child
                            }
                        }
                    }
                }
            } else if (child is ViewGroup) {
                val found = findRecentPassesList(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun showReceiptDetailsDialog(receipt: Receipt) {
        val imageView = ImageView(this)
        imageView.setImageURI(receipt.photoUri)
        imageView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        val textView = TextView(this)
        textView.text = "${receipt.extractedText}\n\nTotal: ₹%.2f\nDate: %s".format(receipt.total, receipt.date)
        textView.setPadding(16, 16, 16, 16)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.addView(imageView)
        container.addView(textView)
        AlertDialog.Builder(this)
            .setTitle("Receipt Details")
            .setView(container)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun deleteReceipt(receipt: Receipt) {
        AlertDialog.Builder(this)
            .setTitle("Delete Receipt")
            .setMessage("Are you sure you want to delete this receipt?")
            .setPositiveButton("Delete") { _, _ ->
                val position = receiptsList.indexOf(receipt)
                if (position != -1) {
                    receiptsList.removeAt(position)
                    receiptsAdapter.notifyItemRemoved(position)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
