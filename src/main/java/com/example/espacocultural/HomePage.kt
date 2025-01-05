package com.example.espacocultural

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.espacocultural.models.Artists
import com.example.espacocultural.models.GlobalVariables
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.ByteArrayOutputStream
import java.util.Locale

class HomePage : AppCompatActivity(),  ArtistsAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArtistsAdapter
    private lateinit var addImage: ImageView
    private var artistsListener: ListenerRegistration? = null
    private var artistsList = mutableListOf<Artists>()

    private val db = FirebaseFirestore.getInstance()

    private var selectedImage: Boolean = false
    private var inOptions: Boolean = false

    private lateinit var addArtist: RelativeLayout

    val CHANNEL_ID = "channelId"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Recupera a configuração de idioma de SharedPreferences
        val sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString("AppLanguage", "pt")
        GlobalVariables.appLanguage = savedLanguage ?: "pt"

        when (GlobalVariables.appLanguage) {
            "pt" -> changeLanguage(Locale("pt"))
            "en" -> changeLanguage(Locale("en"))
            else -> changeLanguage(Locale("es"))
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.home_page)

        // Inicializa o RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Cria e define o adaptador para o RecyclerView
        adapter = ArtistsAdapter(artistsList, this)
        recyclerView.adapter = adapter
        loadArtistsFromFirestore()

        val oldExpositions: RelativeLayout = findViewById(R.id.oldExpositions)

        oldExpositions.setOnClickListener {
            changeScreen(this, OldExpositionsPage::class.java)
        }

        val optionsButton = findViewById<ImageView>(R.id.options_image)
        addArtist = findViewById(R.id.add) // Botão para adicionar obra

        // Card de criar artista
        val outsideCard: FrameLayout = findViewById(R.id.artist_creation_background) // Layout do card
        val leaveButton: RelativeLayout = findViewById(R.id.leave_card) // Botão de sair do card
        val errorPrevention: FrameLayout = findViewById(R.id.error_prevention_background) // Card de prevenção de erros

        if (GlobalVariables.isAdmin) {
            optionsButton.visibility = View.VISIBLE
            addArtist.visibility = View.VISIBLE
        } else {
            optionsButton.visibility = View.GONE
            addArtist.visibility = View.GONE
        }

        addArtist.setOnClickListener {
            outsideCard.visibility = View.VISIBLE

            addImage = findViewById(R.id.add_image)
            val artistName: EditText = findViewById(R.id.artist_create_name)
            val artistBiography: EditText = findViewById(R.id.artist_create_biography)

            val createArtist: Button = findViewById(R.id.create_artist)
            createArtist.setText(R.string.create_artist)

            // Abre a galeria do celular e seleciona imagem
            addImage.setOnClickListener {
                openGallery()
            }

            createArtist.setOnClickListener {
                // Adiciona número e imagem, fazer if (se não tiver imagem ou texto, não adicionar)
                if (addImage.drawable != null && (artistName.text.toString() != "" || artistBiography.text.toString() != "")) {
                    val artist = mapOf(
                        "Nome" to artistName.text.toString(),
                        "Biografia" to artistBiography.text.toString(),
                        "BiografiaEN" to "",
                        "BiografiaES" to "",
                        "imagem" to imageViewToBase64(addImage)
                    )

                    db.collection("artistas").document(artistName.text.toString())
                        .set(artist)

                    // Notificação de criação
                    if (GlobalVariables.notifications) {
                        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
                        builder.setSmallIcon(R.drawable.app_icon)
                            .setContentTitle("Artista novo!")
                            .setContentText("Confira só o artista ${artistName.text.toString()}!")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                        with(NotificationManagerCompat.from(this)) {
                            if (ActivityCompat.checkSelfPermission(
                                    applicationContext,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return@setOnClickListener
                            }
                            notify(1, builder.build())
                        }
                    }

                    outsideCard.visibility = View.GONE
                    artistName.text.clear()
                    artistBiography.text.clear()
                    addImage.setImageDrawable(null)

                    loadArtistsFromFirestore()
                }  else {
                    Toast.makeText(this, "O artista está sem imagem ou algum campo não foi preenchido!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        optionsButton.setOnClickListener {
            // Editar, remover (pegar os botões do RecyclerView)
            if (!inOptions) {
                artistsList.forEach { it.showOptions = true }
                addArtist.visibility = View.GONE
                adapter.notifyDataSetChanged()

                optionsChangeSizeAndImage(20, R.drawable.x_white)
            } else {
                artistsList.forEach { it.showOptions = false }
                addArtist.visibility = View.VISIBLE
                adapter.notifyDataSetChanged()

                optionsChangeSizeAndImage(30, R.drawable.options)
            }

            inOptions = !inOptions
        }

        leaveButton.setOnClickListener {
            // Prevenção de erros
            val errorPreventionText: TextView = findViewById(R.id.error_prevention_text)
            errorPreventionText.setText(R.string.artist_leave_error_prevention)
            errorPrevention.visibility = View.VISIBLE
        }

        val cancelButton: Button = findViewById(R.id.cancel_button) // Botão para cancelar a operação
        val confirmButton: Button = findViewById(R.id.confirm_button) // Botão para confirmar a operação

        cancelButton.setOnClickListener {
            // Cancela a saída e volta à tela de adicionar obra
            errorPrevention.visibility = View.GONE
        }

        confirmButton.setOnClickListener {
            // Confirma a saída e volta à tela das obras
            errorPrevention.visibility = View.GONE
            outsideCard.visibility = View.GONE

            val artistName: EditText = findViewById(R.id.artist_create_name)
            val artistBiography: EditText = findViewById(R.id.artist_create_biography)

            artistName.text.clear()
            artistBiography.text.clear()
            addImage.setImageDrawable(null)
        }

        // Botões barra inferior
        val compassButton = findViewById<Button>(R.id.compassButton)
        val qrButton = findViewById<Button>(R.id.qrButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        compassButton.setOnClickListener{
            changeScreen(this, SalonsPage::class.java)
        }

        qrButton.setOnClickListener{
            changeScreen(this, QrPage::class.java)
        }

        settingsButton.setOnClickListener{
            changeScreen(this, SettingsPage::class.java)
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

    override fun onStart() {
        super.onStart()
        // Recarregar dados do Firestore quando a activity é iniciada
        loadArtistsFromFirestore()
    }

    override fun onStop() {
        super.onStop()
        // Remover o listener para evitar memory leaks
        artistsListener?.remove()
    }

    private fun loadArtistsFromFirestore() {
        artistsListener = db.collection("artistas")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val tempList = mutableListOf<Artists>()

                    for (document in snapshot.documents) {
                        val artistData = document.data
                        if (artistData != null) {
                            val name = artistData["Nome"] as? String?: continue
                            var biography = ""

                            when (GlobalVariables.appLanguage) {
                                "pt" -> biography = artistData["Biografia"] as? String ?: ""
                                "en" -> biography = artistData["BiografiaEN"] as? String ?: ""
                                else -> biography = artistData["BiografiaES"] as? String ?: ""
                            }

                            val base64Image = artistData["imagem"] as? String ?: ""

                            val image = decodeBase64ToDrawable(base64Image)

                            // Crie um objeto Artist com os dados recuperados
                            val artist = Artists(name, biography, image)
                            tempList.add(artist)
                        }
                    }

                    // Atualize a lista e notifique o Adapter
                    artistsList.clear()
                    artistsList.addAll(tempList)
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun openGallery() {
        // Cria um intent para abrir a galeria
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        // Inicia a atividade da galeria com o resultado esperado
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Imagem selecionada com sucesso
            val data: Intent? = result.data
            val selectedImageUri = data?.data

            // Carrega a imagem selecionada no ImageView usando Glide
            selectedImageUri?.let {
                Glide.with(this)
                    .load(it)
                    .into(addImage)

                selectedImage = true
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

    private fun optionsChangeSizeAndImage(size: Int, image: Int) {
        val optionsImage: ImageView = findViewById(R.id.options_image)
        optionsImage.setImageResource(image)

        val params = optionsImage.layoutParams
        val density = resources.displayMetrics.density

        params.width = (size * density).toInt()
        params.height = (size * density).toInt()

        optionsImage.layoutParams = params
    }

    override fun onDeleteIconClick(position: Int) {
        val selectedArtist = artistsList[position]

        val deleteCard: FrameLayout = findViewById(R.id.delete_error_prevention)
        deleteCard.visibility = View.VISIBLE
        val deleteText: TextView = findViewById(R.id.delete_text)
        deleteText.text = getString(R.string.artist_delete_error_prevention) + " " + selectedArtist.name + "?"

        val cancelDeletion: Button = findViewById(R.id.cancel_delete_button)
        val confirmDeletion: Button = findViewById(R.id.confirm_delete_button)

        cancelDeletion.setOnClickListener {
            // Cancela a deleção
            deleteCard.visibility = View.GONE
        }

        confirmDeletion.setOnClickListener {
            // Confirma a deleção
            deleteCard.visibility = View.GONE
            var oneElement: Boolean = false

            if (artistsList.size == 1) {
                oneElement = true
            }

            db.collection("artistas").document(selectedArtist.name)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "${selectedArtist.name} foi deletado com sucesso!", Toast.LENGTH_SHORT).show()
                    loadArtistsFromFirestore()

                    if (oneElement) {
                        artistsList.clear()
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    override fun onEditIconClick(position: Int) {
        val selectedArtist = artistsList[position]
        val docRef = db.collection("artistas").document(selectedArtist.name)

        val outsideCard: FrameLayout = findViewById(R.id.artist_creation_background) // Layout do card
        val leaveButton: RelativeLayout = findViewById(R.id.leave_card) // Botão de sair do card
        val errorPrevention: FrameLayout = findViewById(R.id.error_prevention_background) // Card de prevenção de erros

        outsideCard.visibility = View.VISIBLE

        val artistName: EditText = findViewById(R.id.artist_create_name)
        val artistBiography: EditText = findViewById(R.id.artist_create_biography)

        getFieldFromDatabase(artistName, "Nome", docRef)
        getFieldFromDatabase(artistBiography, "Biografia", docRef)

        addImage = findViewById(R.id.add_image)
        showImageFromDatabase(addImage, docRef)

        val editArtist: Button = findViewById(R.id.create_artist)
        editArtist.setText(R.string.edit_artist)

        // Abre a galeria do celular e seleciona imagem
        addImage.setOnClickListener {
            openGallery()
        }

        editArtist.setOnClickListener {
            // Adiciona número e imagem, fazer if (se não tiver imagem ou texto, não adicionar)
            if (addImage.drawable != null && (artistName.text.toString() != "" || artistBiography.text.toString() != "")) {
                val artist = mapOf(
                    "Nome" to artistName.text.toString(),
                    "Biografia" to artistBiography.text.toString(),
                    "imagem" to imageViewToBase64(addImage)
                )

                docRef.update(artist)
                    .addOnSuccessListener {
                        Toast.makeText(this, "O Artista ${selectedArtist.name} foi atualizado com sucesso", Toast.LENGTH_SHORT).show()
                        // Opcional: Atualize a lista e o adaptador se necessário
                        loadArtistsFromFirestore()

                        outsideCard.visibility = View.GONE
                        artistName.text.clear()
                        artistBiography.text.clear()

                        addImage.setImageDrawable(null)
                    }
            }  else {
                Toast.makeText(this, "O artista está sem imagem ou algum campo não foi preenchido!", Toast.LENGTH_SHORT).show()
            }
        }

        leaveButton.setOnClickListener {
            // Prevenção de erros
            val errorPreventionText: TextView = findViewById(R.id.error_prevention_text)
            errorPreventionText.setText(R.string.artist_edit_error_prevention)
            errorPrevention.visibility = View.VISIBLE
        }

        val cancelButton: Button = findViewById(R.id.cancel_button) // Botão para cancelar a operação
        val confirmButton: Button = findViewById(R.id.confirm_button) // Botão para confirmar a operação

        cancelButton.setOnClickListener {
            // Cancela a saída e volta à tela de adicionar obra
            errorPrevention.visibility = View.GONE
        }

        confirmButton.setOnClickListener {
            // Confirma a saída e volta à tela das obras
            errorPrevention.visibility = View.GONE
            outsideCard.visibility = View.GONE

            artistName.text.clear()
            artistBiography.text.clear()
            addImage.setImageDrawable(null)
        }
    }

    private fun getFieldFromDatabase(input: EditText, field: String, dbRef: DocumentReference) {
        dbRef.get().addOnSuccessListener {
            if (it != null) {
                input.setText(it.getString(field))
            } else {
                Log.d("Database", "No such document")
            }
        }
    }

    private fun showImageFromDatabase(image: ImageView, dbRef: DocumentReference) {
        dbRef.get().addOnSuccessListener {
            if (it != null) {
                val imageInString = it.getString("imagem")
                image.setImageDrawable(imageInString?.let { it1 -> decodeBase64ToDrawable(it1) })
            } else {
                Log.d("Database", "No such document")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "First channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Test description for my channel"

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}