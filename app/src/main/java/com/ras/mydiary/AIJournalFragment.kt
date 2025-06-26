package com.ras.mydiary

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class AIJournalFragment : Fragment() {

    // Declare views at the class level to be accessible throughout the fragment's lifecycle
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var summaryTextView: TextView
    private lateinit var moodDropdown: AutoCompleteTextView

    // onCreateView is where you inflate your layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ai_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.aijournal)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inputEditText = view.findViewById(R.id.inputEditText)
        sendButton = view.findViewById(R.id.sendButton)
        summaryTextView = view.findViewById(R.id.summaryTextView)
        moodDropdown = view.findViewById(R.id.mood_dropdown_autocomplete_text_view)

        val emotions = arrayOf(
            "angry", "sad", "happy", "neutral",
            "irritated", "excited", "nervous", "tired", "confused"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, emotions)

        moodDropdown.setAdapter(adapter)

        sendButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()

            if (userInput.isNotEmpty()) {
                summarizePrompt(userInput,
                    onSuccess = { summary ->
                        activity?.runOnUiThread {
                            summaryTextView.text = summary
                        }
                    },
                    onError = { error ->
                        activity?.runOnUiThread {
                            Log.d("AIJournal", "Error: $error")
                            Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Please enter something to summarize.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun summarizePrompt(prompt: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val client = OkHttpClient()

        val json = JSONObject().put("prompt", prompt).toString()
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("http://192.168.1.3:5000/summarize")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onError("Failed to connect: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        onError("Unexpected response: ${response.code}. Message: ${response.message}")
                        return
                    }

                    val responseBody = response.body?.string()
                    try {
                        val summary = responseBody?.let { it1 -> JSONObject(it1).getString("summary") }
                        if (summary != null) {
                            onSuccess(summary)
                        } else {
                            onError("Summary not found in response.")
                        }
                    } catch (e: Exception) {
                        onError("Failed to parse response: ${e.message}")
                    }
                }
            }
        })
    }
}