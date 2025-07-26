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
import com.shashank.paymentappui.OpenAIMessage
import com.shashank.paymentappui.OpenAIRequest
import com.shashank.paymentappui.OpenAIResponse
import android.view.View
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.ImageButton
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView
import android.speech.RecognizerIntent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import android.media.MediaRecorder
import android.provider.MediaStore
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class ChatMessage(val sender: String, val text: String)
data class Transaction(val shopName: String, val amount: Double, val date: String)

class MainActivity : AppCompatActivity() {
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    // Add some default transactions (e.g., Amazon, Big Market, Swizzy)
    private val defaultTransactions = listOf(
        Transaction("Big Market", 5250.0, "07 Jul 2024"),
        Transaction("Amazone", 2000.0, "06 Jul 2024"),
        Transaction("Swizzy", 3000.0, "05 Jul 2024")
    )
    private val transactionList = mutableListOf<Transaction>().apply { addAll(defaultTransactions) }
    private var defaultsRemoved = false
    private val REQUEST_CODE_SPEECH = 1001
    private val REQUEST_CODE_MIC_PERMISSION = 1002
    private var recorder: MediaRecorder? = null
    private lateinit var audioFile: File
    private val openAiApiKey = "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" // <-- Paste your OpenAI API key here
    private val REQUEST_CODE_CAMERA = 2001
    private var cameraImageUri: Uri? = null

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

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        chatAdapter = ChatAdapter(chatList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

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
                    searchLovable(query)
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
                    searchLovable(query)
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

        cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_CAMERA)
            } else {
                openCameraForBill()
            }
        }

        // Set up mini camera click listeners in Recent Passes section
        setupMiniCamListeners()
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.RECORD_AUDIO
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_CODE_MIC_PERMISSION)
            return false
        }
        return true
    }

    private fun startRecording() {
        audioFile = File(externalCacheDir, "audio.mp4")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    private fun transcribeAudio(apiKey: String, audioFile: File, callback: (String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull()))
                .addFormDataPart("model", "whisper-1")
                .build()

            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val result = response.body?.string()
                if (!response.isSuccessful) {
                    // Show error code and message
                    callback("API error: ${response.code} ${response.message}\n$result")
                    return@launch
                }
                val text = JSONObject(result ?: "").optString("text")
                callback(text)
            } catch (e: Exception) {
                callback("Exception: ${e.message}")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Toast.makeText(this, "onRequestPermissionsResult called", Toast.LENGTH_SHORT).show()
        if (requestCode == REQUEST_CODE_MIC_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startRecording()
                val micButton = findViewById<ImageButton>(R.id.micButton)
                micButton.setImageResource(R.drawable.ic_mic)
                Toast.makeText(this, "Recording... Tap again to stop.", Toast.LENGTH_SHORT).show()
            } else {
                // Show which permissions are missing
                val denied = permissions.zip(grantResults.toTypedArray())
                    .filter { it.second != PackageManager.PERMISSION_GRANTED }
                    .joinToString { it.first }
                Toast.makeText(this, "Denied: $denied", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCameraForBill() {
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
            if (spokenText.isNotBlank()) {
                searchLovable(spokenText)
            }
        } else if (requestCode == REQUEST_CODE_CAMERA && resultCode == RESULT_OK) {
            cameraImageUri?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
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
                // Try to parse shop name and amount
                val (shop, amount) = parseShopAndAmount(text)
                showAddTransactionDialogPrefill(shop, amount)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Could not extract text from image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun parseShopAndAmount(text: String): Pair<String, Double> {
        // Simple heuristics: first wordy line is shop, first number with decimal is amount
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var shop = ""
        var amount = 0.0
        for (line in lines) {
            if (shop.isEmpty() && line.any { it.isLetter() }) {
                shop = line.take(32)
            }
            val amtMatch = Regex("[₹$]?([0-9]+[.,][0-9]{2,})").find(line)
            if (amtMatch != null) {
                val amtStr = amtMatch.groupValues[1].replace(",", ".")
                amount = amtStr.toDoubleOrNull() ?: 0.0
            }
            if (shop.isNotEmpty() && amount > 0) break
        }
        return Pair(shop, amount)
    }

    private fun showAddTransactionDialogPrefill(shop: String, amount: Double) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val shopNameInput = dialogView.findViewById<EditText>(R.id.shopNameInput)
        val amountInput = dialogView.findViewById<EditText>(R.id.amountInput)
        shopNameInput.setText(shop)
        amountInput.setText(if (amount > 0) amount.toString() else "")
        AlertDialog.Builder(this)
            .setTitle("Add Recent Pass from Bill")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val shopFinal = shopNameInput.text.toString()
                val amountFinal = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                if (shopFinal.isNotBlank() && amountFinal > 0) {
                    if (!defaultsRemoved) {
                        transactionList.clear()
                        defaultsRemoved = true
                    }
                    transactionList.add(Transaction(shopFinal, amountFinal, date))
                    updateRecentPassesUI()
                } else {
                    Toast.makeText(this, "Enter valid details", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun updateChat() {
        chatRecyclerView.visibility = View.VISIBLE
        chatAdapter.notifyDataSetChanged()
        chatRecyclerView.scrollToPosition(chatList.size - 1)
    }

    private fun searchLovable(query: String) {
        Toast.makeText(this, "Asking AI: $query", Toast.LENGTH_SHORT).show()
        chatList.add(ChatMessage("You", query))
        updateChat()
        val request = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(OpenAIMessage(content = query))
        )
        RetrofitClient.openAiApi.getChatCompletion(request).enqueue(object : retrofit2.Callback<OpenAIResponse> {
            override fun onResponse(call: retrofit2.Call<OpenAIResponse>, response: retrofit2.Response<OpenAIResponse>) {
                if (response.isSuccessful) {
                    val aiResponse = response.body()
                    val answer = aiResponse?.choices?.firstOrNull()?.message?.content
                    if (!answer.isNullOrBlank()) {
                        chatList.add(ChatMessage("AI", answer))
                    } else {
                        chatList.add(ChatMessage("AI", "Sorry, I don't have an answer for that yet."))
                    }
                } else {
                    chatList.add(ChatMessage("AI", "API error: ${response.code()}"))
                }
                updateChat()
            }
            override fun onFailure(call: retrofit2.Call<OpenAIResponse>, t: Throwable) {
                chatList.add(ChatMessage("AI", "Network error: ${t.message}"))
                updateChat()
            }
        })
    }
}
