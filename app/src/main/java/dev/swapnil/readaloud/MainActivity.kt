package dev.swapnil.readaloud

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import dev.swapnil.readaloud.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var outputTextView: TextView

    private val READ_REQUEST_CODE = 42
    private val PRIMARY = "primary"
    private val LOCAL_STORAGE = "/storage/self/primary"
    private val EXT_STORAGE = "/storage/self"

    private val COLON = ":"

    private lateinit var permissionlauncher: ActivityResultLauncher<Array<String>>
    private var isReadPermissionGranted = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val outputTextView: TextView = findViewById(R.id.output_text)
        outputTextView.movementMethod = ScrollingMovementMethod()

        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }


        val getContent = registerForActivityResult(ActivityResultContracts.OpenDocument()){
            uri: Uri? ->
            permissionlauncher.launch(arrayOf(
                "application/msword", //.doc (Microsoft Word)
                "application/pdf" //.pdf (Pdf file)
            ))
        }
        fun pickDocument() = getContent.launch(arrayOf("*/*"))



        binding.fab.setOnClickListener { view ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.setType("*/*")
            startActivityForResult(intent,READ_REQUEST_CODE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.let { uri ->
                // Toast.makeText(this, uri.path, Toast.LENGTH_SHORT).show()
                uri.path?.let { Log.v("URI", it) }
                readPdfFile(uri)
            }
        }
    }


    fun readPdfFile(uri: Uri?) {
        uri?.path?.let { uriPath ->
            val fullPath = if (uriPath.contains(PRIMARY)) {
                LOCAL_STORAGE + uriPath.split(COLON)[1]
            } else {
                EXT_STORAGE + uriPath.split(COLON)[1]
            }
            Log.v("URI", "$uriPath $fullPath")
            try {
                PdfReader(fullPath).use { pdfReader ->
                    val stringParser = PdfTextExtractor.getTextFromPage(pdfReader, 1).trim()
                    outputTextView.text = stringParser
                    textToSpeech.speak(stringParser, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun <T : PdfReader, R> T.use(block: (T) -> R): R {
        return try {
            block(this)
        } finally {
            this.close()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


}


