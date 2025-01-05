package com.example.espacocultural

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.espacocultural.models.GlobalVariables
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ArtistInfoPage : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var artistId: String
    private lateinit var tts: TextToSpeech
    private lateinit var playButton: ImageView

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale(GlobalVariables.appLanguage))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language is not supported")
            } else {
                playButton.isEnabled = true
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        when (GlobalVariables.appLanguage) {
            "pt" -> changeLanguage(Locale("pt"))
            "en" -> changeLanguage(Locale("en"))
            else -> changeLanguage(Locale("es"))
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.artist_info_page)

        // De qual obra eu vim?
        // Recuperar os valores passados pela Intent
        if (intent.hasExtra("artistId")) {
            artistId = intent.getStringExtra("artistId") ?: ""
            Log.d("ArtistInfoPage", "artId recebido: $artistId")
        } else {
            Log.e("ArtistInfoPage", "Os extras 'artId' ou 'salonId' não foram passados na Intent")
        }

        // Botões superiores
        val returnButton = findViewById<Button>(R.id.return_button)
        val optionsButton = findViewById<ConstraintLayout>(R.id.options_button)
        playButton = findViewById(R.id.play)

        returnButton.setOnClickListener {
            changeScreen(this, GlobalVariables.lastPage)
        }

        if (GlobalVariables.isAdmin) {
            optionsButton.visibility = View.VISIBLE
        } else {
            optionsButton.visibility = View.GONE
        }

        optionsButton.setOnClickListener {
            // Editar, remover
        }

        // Carregamento das obras
        val progressBar = findViewById<ProgressBar>(R.id.loader)
        progressBar.visibility = View.VISIBLE

        val mainContainer = findViewById<LinearLayout>(R.id.mainContainer)
        mainContainer.visibility = View.GONE

        // Informações da Obra
        val artistName = findViewById<TextView>(R.id.artist_name)
        val artistBiography = findViewById<TextView>(R.id.artist_biography)
        val artistImage = findViewById<ShapeableImageView>(R.id.artist_image)

        // Banco de dados
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("artistas").document(artistId)

        docRef.get().addOnSuccessListener {
            if (it != null) {
                val name = it.data?.get("Nome")?.toString()
                var biography = ""

                when (GlobalVariables.appLanguage) {
                    "pt" -> biography = it.data?.get("Biografia").toString()
                    "en" -> biography = it.data?.get("BiografiaEN").toString()
                    else -> biography = it.data?.get("BiografiaES").toString()
                }

                val image = it.data?.get("imagem").toString()

                val imageBitmap = decodeBase64ToBitmap(image)

                artistName.text = name
                artistBiography.text = biography
                displayBitmapInImageView(imageBitmap, artistImage) // Coloca a imagem em miniatura na obra

                progressBar.visibility = View.GONE
                mainContainer.visibility = View.VISIBLE
            }
        }
            .addOnFailureListener {
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.VISIBLE
                mainContainer.visibility = View.GONE
            }

        tts = TextToSpeech(this, this)
        playButton.setOnClickListener {
            if (!tts.isSpeaking) {
                speakOut(artistBiography.text.toString())
                val newImage = resources.getDrawable(R.drawable.pause)
                playButton.setImageDrawable(newImage)
            } else {
                tts.stop()
                val newImage = resources.getDrawable(R.drawable.play)
                playButton.setImageDrawable(newImage)
            }
        }

    }

    fun changeScreen(activity: Activity, clasS: Class<*>?) {
        GlobalVariables.lastPage = activity::class.java
        val intent = Intent(activity, clasS)
        startActivity(intent)
        activity.finish()
        activity.overridePendingTransition(0, 0)
    }

    private fun changeLanguage(locale: Locale) {
        val resources = this.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    // Função para decodificar a string Base64 em um objeto Bitmap
    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        val decodedBytes: ByteArray = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    // Função para exibir o Bitmap em uma ImageView
    private fun displayBitmapInImageView(bitmap: Bitmap?, imageView: ImageView) {
        bitmap?.let {
            imageView.setImageBitmap(it)
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onDestroy() {
        if (tts.isSpeaking) {
            tts.stop()
        }
        tts.shutdown()
        super.onDestroy()
    }
}