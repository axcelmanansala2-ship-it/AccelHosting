package com.accel.hosting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.accel.hosting.api.ApiClient
import com.accel.hosting.databinding.ActivityUploadBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private var selectedUri: Uri? = null
    private var selectedFileName: String = ""

    companion object {
        private const val PICK_FILE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Upload Bot"

        ApiClient.init(this)

        binding.btnPickFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/zip", "application/x-zip-compressed",
                    "text/x-python", "text/plain", "application/octet-stream"
                ))
            }
            startActivityForResult(intent, PICK_FILE)
        }

        binding.btnUpload.setOnClickListener { doUpload() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            selectedFileName = getFileName(selectedUri) ?: "unknown"
            val ext = selectedFileName.substringAfterLast('.', "")
            if (ext !in listOf("py", "zip")) {
                Toast.makeText(this, "Only .py and .zip files are supported", Toast.LENGTH_SHORT).show()
                selectedUri = null
                selectedFileName = ""
                binding.tvSelectedFile.text = "No file selected"
                return
            }
            binding.tvSelectedFile.text = selectedFileName

            if (binding.etBotName.text.isBlank()) {
                binding.etBotName.setText(selectedFileName.substringBeforeLast('.'))
            }
        }
    }

    private fun getFileName(uri: Uri?): String? {
        uri ?: return null
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) name = cursor.getString(idx)
                }
            }
        }
        return name ?: uri.path?.substringAfterLast('/')
    }

    private fun doUpload() {
        val uri = selectedUri
        val botName = binding.etBotName.text.toString().trim()
        when {
            uri == null -> Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show()
            botName.isBlank() -> Toast.makeText(this, "Enter a bot name", Toast.LENGTH_SHORT).show()
            else -> {
                setLoading(true)
                lifecycleScope.launch {
                    try {
                        val tmpFile = File(cacheDir, selectedFileName)
                        contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
                        }

                        val mediaType = if (selectedFileName.endsWith(".zip"))
                            "application/zip" else "text/x-python"
                        val fileBody = tmpFile.asRequestBody(mediaType.toMediaTypeOrNull())
                        val filePart = MultipartBody.Part.createFormData("file", selectedFileName, fileBody)
                        val namePart = botName.toRequestBody("text/plain".toMediaTypeOrNull())

                        ApiClient.service.uploadBot(namePart, filePart)
                        tmpFile.delete()

                        Toast.makeText(this@UploadActivity,
                            "$botName uploaded! Installing dependencies…", Toast.LENGTH_LONG).show()
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@UploadActivity,
                            "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        setLoading(false)
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnUpload.isEnabled = !loading
        binding.btnPickFile.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
