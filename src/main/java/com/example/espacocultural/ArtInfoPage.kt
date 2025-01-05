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
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.espacocultural.models.GlobalVariables
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.FirebaseAuthCredentialsProvider
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class ArtInfoPage : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var artId: String
    private var salonId: Int? = null
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
        setContentView(R.layout.art_info_page)

        // De qual obra eu vim?
        // Recuperar os valores passados pela Intent
        if (intent.hasExtra("artId") && intent.hasExtra("salonId")) {
            artId = intent.getStringExtra("artId") ?: ""
            salonId = intent.getIntExtra("salonId", -1)
            Log.d("ArtInfoPage", "artId recebido: $artId")
            Log.d("ArtInfoPage", "salonId recebido: $salonId")
        } else {
            if (intent.hasExtra("artId") && !intent.hasExtra("salonId")) {
                artId = intent.getStringExtra("artId") ?: ""
            }
            Log.e("ArtInfoPage", "Os extras 'artId' ou 'salonId' não foram passados na Intent")
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

        val mainContainer = findViewById<RelativeLayout>(R.id.mainContainer)
        mainContainer.visibility = View.GONE

        // Informações da Obra
        val artName = findViewById<TextView>(R.id.art_name)
        val artYear = findViewById<TextView>(R.id.art_year)
        val artAuthor = findViewById<TextView>(R.id.art_author)
        val artDescription = findViewById<TextView>(R.id.art_description)
        val artImage = findViewById<ImageView>(R.id.art_image)
        val expandedArtImage = findViewById<ImageView>(R.id.expanded_art_image) // Inicializa antes para expansão

        if (GlobalVariables.lastPage != OldExpositionsPage::class.java) {
            // Banco de dados
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("saloes").document("salao $salonId").collection("obras").document(artId)

            docRef.get().addOnSuccessListener {
                if (it != null) {
                    val name = it.data?.get("Nome da obra")?.toString()
                    val year = it.data?.get("Ano")?.toString()
                    val author = it.data?.get("Autor")?.toString()
                    var description = ""

                    when (GlobalVariables.appLanguage) {
                        "pt" -> description = it.data?.get("Descrição").toString()
                        "en" -> description = it.data?.get("DescriçãoEN").toString()
                        else -> description = it.data?.get("DescriçãoES").toString()
                    }

                    val image = it.data?.get("imagem").toString()

                    val imageBitmap = decodeBase64ToBitmap(image)

                    artName.text = name
                    artYear.text = " - " + year
                    artAuthor.text = author
                    artDescription.text = description
                    displayBitmapInImageView(imageBitmap, artImage) // Coloca a imagem em miniatura na obra
                    displayBitmapInImageView(imageBitmap, expandedArtImage) // Coloca a imagem expandida

                    progressBar.visibility = View.GONE
                    mainContainer.visibility = View.VISIBLE
                }
            }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.VISIBLE
                    mainContainer.visibility = View.GONE
                }

        } else {
            // Banco de dados
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("antigas").document(artId)

            docRef.get().addOnSuccessListener {
                if (it != null) {
                    val name = it.data?.get("Nome da obra")?.toString()
                    val year = it.data?.get("Ano")?.toString()
                    val author = it.data?.get("Autor")?.toString()
                    var description = ""

                    when (GlobalVariables.appLanguage) {
                        "pt" -> description = it.data?.get("Descrição").toString()
                        "en" -> description = it.data?.get("DescriçãoEN").toString()
                        else -> description = it.data?.get("DescriçãoES").toString()
                    }

                    val image = it.data?.get("imagem").toString()

                    val imageBitmap = decodeBase64ToBitmap(image)

                    artName.text = name
                    artYear.text = " - " + year
                    artAuthor.text = author
                    artDescription.text = description
                    displayBitmapInImageView(imageBitmap, artImage) // Coloca a imagem em miniatura na obra
                    displayBitmapInImageView(imageBitmap, expandedArtImage) // Coloca a imagem expandida

                    progressBar.visibility = View.GONE
                    mainContainer.visibility = View.VISIBLE
                }
            }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.VISIBLE
                    mainContainer.visibility = View.GONE
                }
        }

        tts = TextToSpeech(this, this)
        playButton.setOnClickListener {
            if (!tts.isSpeaking) {
                speakOut(artDescription.text.toString())
                val newImage = resources.getDrawable(R.drawable.pause)
                playButton.setImageDrawable(newImage)
            } else {
                tts.stop()
                val newImage = resources.getDrawable(R.drawable.play)
                playButton.setImageDrawable(newImage)
            }
        }

        // Tela expandida
        val expandedArt: FrameLayout = findViewById(R.id.expanded_art) // Tela da expansão
        val leaveExpansion: RelativeLayout = findViewById(R.id.leave_expansion) // Botão para sair de expansão
        val expandButton: ImageButton = findViewById(R.id.expand_button)

        expandButton.setOnClickListener {
            // Expande a imagem
            if (progressBar.visibility == View.GONE) {
                expandedArt.visibility = View.VISIBLE
            }
        }

        artImage.setOnClickListener {
            // Expande a imagem
            if (progressBar.visibility == View.GONE) {
                expandedArt.visibility = View.VISIBLE
            }
        }

        leaveExpansion.setOnClickListener {
            expandedArt.visibility = View.GONE
        }
    }

    fun changeScreen(activity: Activity, clasS: Class<*>?) {
        GlobalVariables.lastPage = activity::class.java
        val intent = Intent(activity, clasS)
        intent.putExtra("salonId", salonId)
        startActivity(intent)
        activity.finish()
        activity.overridePendingTransition(0, 0)
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

    private fun changeLanguage(locale: Locale) {
        val resources = this.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
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