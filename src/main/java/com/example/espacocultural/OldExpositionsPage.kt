package com.example.espacocultural

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.espacocultural.models.Artists
import com.example.espacocultural.models.Arts
import com.example.espacocultural.models.GlobalVariables
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.ByteArrayOutputStream
import java.util.Locale

class OldExpositionsPage : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OldExpoAdapter
    private var artsList = mutableListOf<Arts>()
    private var artsListener: ListenerRegistration? = null

    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        when (GlobalVariables.appLanguage) {
            "pt" -> changeLanguage(Locale("pt"))
            "en" -> changeLanguage(Locale("en"))
            else -> changeLanguage(Locale("es"))
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.old_expositions_page)

        // Inicializa o RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Cria e define o adaptador para o RecyclerView
        adapter = OldExpoAdapter(artsList)
        recyclerView.adapter = adapter
        loadOldExpoFromFirestore()

        // Botões superiores
        val returnButton = findViewById<Button>(R.id.return_button)

        returnButton.setOnClickListener {
            changeScreen(this, HomePage::class.java)
        }
    }

    override fun onStart() {
        super.onStart()
        // Recarregar dados do Firestore quando a activity é iniciada
        loadOldExpoFromFirestore()
    }

    fun changeScreen(activity: Activity, clasS: Class<*>?) {
        GlobalVariables.lastPage = activity::class.java
        val intent = Intent(activity, clasS)
        startActivity(intent)
        activity.finish()
        activity.overridePendingTransition(0, 0)
    }

    private fun loadOldExpoFromFirestore() {
        artsListener = db.collection("antigas")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val tempList = mutableListOf<Arts>()

                    for (document in snapshot.documents) {
                        val artData = document.data
                        if (artData != null && artData["Nome da obra"] != "ignore") {
                            val name = artData["Nome da obra"] as String
                            val year = artData["Ano"] as String
                            val author = artData["Autor"] as String
                            var description = ""

                            when (GlobalVariables.appLanguage) {
                                "pt" -> description = artData["Descrição"] as String
                                "en" -> description = artData["DescriçãoEN"] as String
                                else -> description = artData["DescriçãoES"] as String
                            }

                            val base64Image = artData["imagem"] as String

                            val image = decodeBase64ToDrawable(base64Image)

                            // Crie um objeto Art com os dados recuperados
                            val art = Arts(name, year, author, description, image)
                            tempList.add(art)
                        }
                    }

                    // Atualize a lista e notifique o Adapter
                    artsList.clear()
                    artsList.addAll(tempList)
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun imageViewToBase64(imageView: ImageView): String {
        // Obtém o drawable da imageView
        val drawable = imageView.drawable as BitmapDrawable
        // Obtém o bitmap do drawable
        val bitmap = drawable.bitmap
        // Converte o bitmap em uma string Base64
        return bitmapToBase64(bitmap)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun decodeBase64ToDrawable(base64Image: String): Drawable? {
        val decodedBytes: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
        val bitmap: Bitmap? = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        return bitmap?.let { BitmapDrawable(resources, it) }
    }

    private fun changeLanguage(locale: Locale) {
        val resources = this.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}