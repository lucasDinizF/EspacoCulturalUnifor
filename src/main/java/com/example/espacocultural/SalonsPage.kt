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
import android.text.InputFilter
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.espacocultural.models.Arts
import com.example.espacocultural.models.GlobalVariables
import com.example.espacocultural.models.Salons
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import java.io.ByteArrayOutputStream
import java.util.Locale

class SalonsPage : AppCompatActivity(), SalonsAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SalonsAdapter
    private lateinit var addImage: ImageView
    private var salonsListener: ListenerRegistration? = null
    private var salonsList = mutableListOf<Salons>()

    private val db = FirebaseFirestore.getInstance()

    private var selectedImage: Boolean = false
    private var inOptions: Boolean = false

    val CHANNEL_ID = "channelId"

    override fun onCreate(savedInstanceState: Bundle?) {
        when (GlobalVariables.appLanguage) {
            "pt" -> changeLanguage(Locale("pt"))
            "en" -> changeLanguage(Locale("en"))
            else -> changeLanguage(Locale("es"))
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.salons_page)
        salonsList.clear()

        // Inicializa o RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Cria e define o adaptador para o RecyclerView
        adapter = SalonsAdapter(salonsList, this)
        recyclerView.adapter = adapter
        loadSalonsFromFirestore()

        // Botões Barra Superior & Adicionar
        val optionsButton: ConstraintLayout = findViewById(R.id.options_button) // Botão de Editar e Remover
        val addSalon: RelativeLayout = findViewById(R.id.add) // Botão para adicionar salão

        // Card de criar salão
        val outsideCard: FrameLayout = findViewById(R.id.salon_creation_background) // Layout do card
        val leaveButton: RelativeLayout = findViewById(R.id.leave_card) // Botão de sair do card
        val errorPrevention: FrameLayout = findViewById(R.id.error_prevention_background) // Card de prevenção de erros

        if (GlobalVariables.isAdmin) {
            optionsButton.visibility = View.VISIBLE
            addSalon.visibility = View.VISIBLE
        } else {
            optionsButton.visibility = View.GONE
            addSalon.visibility = View.GONE
        }

        val dbSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        db.firestoreSettings = dbSettings

        // Clicou no "+" para adicionar salão
        addSalon.setOnClickListener {
            outsideCard.visibility = View.VISIBLE

            addImage = findViewById(R.id.add_image)
            val salonNumber: EditText = findViewById(R.id.salon_creation_number)
            val createSalon: Button = findViewById(R.id.create_salon)
            createSalon.setText(R.string.create_salon)

            configEditTextToOnlyInteger(salonNumber)

            // Abre a galeria do celular e seleciona imagem
            addImage.setOnClickListener {
                openGallery()
            }

            createSalon.setOnClickListener {
                // Adiciona número e imagem, fazer if (se não tiver imagem ou texto, não adicionar)
                if (addImage.drawable != null && salonNumber.text.toString() != "") {
                    val salon = mapOf(
                        "Numero" to salonNumber.text.toString(),
                        "imagem" to imageViewToBase64(addImage)
                    )

                    db.collection("saloes").document("salao " + salonNumber.text.toString())
                        .set(salon)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Salão criado com sucesso!", Toast.LENGTH_SHORT).show()
                        }

                    val art = mapOf(
                        "Ano" to "0000",
                        "Autor" to "ignore",
                        "Descrição" to "ignore",
                        "Nome da obra" to "ignore",
                        "imagem" to imageViewToBase64(addImage)
                    )

                    val docRef = db.collection("saloes").document("salao " + salonNumber.text.toString())

                    docRef.collection("obras").document("ignore")
                        .set(art)

                    // Notificação de criação
                    if (GlobalVariables.notifications) {
                        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
                        builder.setSmallIcon(R.drawable.app_icon)
                            .setContentTitle("Salão novo!")
                            .setContentText("Confira só o nosso salão ${salonNumber.text.toString()} e veja as obras!")
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
                    salonNumber.text.clear()
                    addImage.setImageDrawable(null)

                    loadSalonsFromFirestore()
                } else {
                    Toast.makeText(this, "O salão está sem imagem ou nome!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        optionsButton.setOnClickListener {
            // Editar, remover (pegar os botões do RecyclerView)
            if (!inOptions) {
                salonsList.forEach { it.showOptions = true }
                adapter.notifyDataSetChanged()

                optionsChangeSizeAndImage(20, R.drawable.x_white)
            } else {
                salonsList.forEach { it.showOptions = false }
                adapter.notifyDataSetChanged()

                optionsChangeSizeAndImage(30, R.drawable.options)
            }

            inOptions = !inOptions
        }

        leaveButton.setOnClickListener {
            // Prevenção de erros
            val errorPreventionText: TextView = findViewById(R.id.error_prevention_text)
            errorPreventionText.text = "Você tem certeza que deseja sair da criação do salão?"
            errorPrevention.visibility = View.VISIBLE
        }

        val cancelButton: Button = findViewById(R.id.cancel_button) // Botão para cancelar a operação
        val confirmButton: Button = findViewById(R.id.confirm_button) // Botão para confirmar a operação

        cancelButton.setOnClickListener {
            // Cancela a saída e volta à tela de adicionar salão
            errorPrevention.visibility = View.GONE
        }

        confirmButton.setOnClickListener {
            // Confirma a saída e volta à tela dos salões
            errorPrevention.visibility = View.GONE
            outsideCard.visibility = View.GONE

            val salonNumber: EditText = findViewById(R.id.salon_creation_number)
            salonNumber.text.clear()
        }

        // Botões Barra Inferior
        val homeButton = findViewById<Button>(R.id.homeButton)
        val qrButton = findViewById<Button>(R.id.qrButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        homeButton.setOnClickListener{
            changeScreen(this, HomePage::class.java)
        }

        qrButton.setOnClickListener{
            changeScreen(this, QrPage::class.java)
        }

        settingsButton.setOnClickListener{
            changeScreen(this, SettingsPage::class.java)
        }
    }

    override fun onStart() {
        super.onStart()
        // Recarregar dados do Firestore quando a activity é iniciada
        loadSalonsFromFirestore()
    }

    override fun onStop() {
        super.onStop()
        // Remover o listener para evitar memory leaks
        salonsListener?.remove()
    }

    private fun loadSalonsFromFirestore() {
        salonsListener = db.collection("saloes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val tempList = mutableListOf<Salons>()

                    for (document in snapshot.documents) {
                        val salonData = document.data
                        if (salonData != null) {
                            val idString = salonData["Numero"] as? String ?: continue
                            val base64Image = salonData["imagem"] as? String ?: ""

                            val name = "Salão $idString"
                            val image = decodeBase64ToDrawable(base64Image)
                            val id = idString.toInt()

                            // Crie um objeto Salon com os dados recuperados
                            val salon = Salons(id, name, image, false)
                            tempList.add(salon)
                        }
                    }

                    // Atualize a lista e notifique o Adapter
                    salonsList.clear()
                    salonsList.addAll(tempList)
                    adapter.notifyDataSetChanged()
                }
            }
    }

    fun changeScreen(activity: Activity, clasS: Class<*>?) {
        GlobalVariables.lastPage = activity::class.java
        val intent = Intent(activity, clasS)
        startActivity(intent)
        activity.finish()
        activity.overridePendingTransition(0, 0); // Definindo nenhuma animação
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

    fun configEditTextToOnlyInteger(editText: EditText) {
        val inputFilter = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (!Character.isDigit(source[i])) {
                    return@InputFilter ""
                }
            }
            null
        }

        val maxLength = 3 // Defina o número máximo de dígitos permitidos (opcional)

        // Aplica o filtro de entrada para aceitar apenas números inteiros
        editText.filters = arrayOf(inputFilter)

        // Define o número máximo de caracteres (opcional)
        editText.filters += InputFilter.LengthFilter(maxLength)
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
        val selectedSalon = salonsList[position]

        val deleteCard: FrameLayout = findViewById(R.id.delete_error_prevention)
        deleteCard.visibility = View.VISIBLE
        val deleteText: TextView = findViewById(R.id.delete_text)
        deleteText.text = getString(R.string.salon_delete_error_prevention) + " " + selectedSalon.id + "?"

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

            if (salonsList.size == 1) {
                oneElement = true
            }

            db.collection("saloes").document("salao ${selectedSalon.id}")
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "${selectedSalon.name} foi deletado com sucesso!", Toast.LENGTH_SHORT).show()
                    loadSalonsFromFirestore()

                    if (oneElement) {
                        salonsList.clear()
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    override fun onEditIconClick(position: Int) {
        val selectedSalon = salonsList[position]
        val docRef = db.collection("saloes").document("salao ${selectedSalon.id}")

        val outsideCard: FrameLayout = findViewById(R.id.salon_creation_background) // Layout do card
        val leaveButton: RelativeLayout = findViewById(R.id.leave_card) // Botão de sair do card
        val errorPrevention: FrameLayout = findViewById(R.id.error_prevention_background) // Card de prevenção de erros

        outsideCard.visibility = View.VISIBLE

        val salonNumber: EditText = findViewById(R.id.salon_creation_number)
        getFieldFromDatabase(salonNumber, "Numero", docRef)

        addImage = findViewById(R.id.add_image)
        showImageFromDatabase(addImage, docRef)

        val editSalon: Button = findViewById(R.id.create_salon)
        editSalon.setText(R.string.edit_salon)

        configEditTextToOnlyInteger(salonNumber)

        // Abre a galeria do celular e seleciona imagem
        addImage.setOnClickListener {
            openGallery()
        }

        editSalon.setOnClickListener {
            // Adiciona número e imagem, fazer if (se não tiver imagem ou texto, não adicionar)
            if (addImage.drawable != null && salonNumber.text.toString() != "") {
                val salon = mapOf(
                    "Numero" to salonNumber.text.toString(),
                    "imagem" to imageViewToBase64(addImage)
                )

                docRef.update(salon)
                    .addOnSuccessListener {
                        Toast.makeText(this, "O Salão ${selectedSalon.id} atualizado com sucesso", Toast.LENGTH_SHORT).show()
                        // Opcional: Atualize a lista e o adaptador se necessário
                        loadSalonsFromFirestore()

                        outsideCard.visibility = View.GONE
                        salonNumber.text.clear()

                        addImage.setImageDrawable(null)
                    }
            } else {
                Toast.makeText(this, "O salão está sem imagem ou nome!", Toast.LENGTH_SHORT).show()
            }
        }

        leaveButton.setOnClickListener {
            // Prevenção de erros
            val errorPreventionText: TextView = findViewById(R.id.error_prevention_text)
            errorPreventionText.text = "Você tem certeza que deseja sair da edição do salão?"
            errorPrevention.visibility = View.VISIBLE
        }

        val cancelButton: Button = findViewById(R.id.cancel_button) // Botão para cancelar a operação
        val confirmButton: Button = findViewById(R.id.confirm_button) // Botão para confirmar a operação

        cancelButton.setOnClickListener {
            // Cancela a saída e volta à tela de adicionar salão
            errorPrevention.visibility = View.GONE
        }

        confirmButton.setOnClickListener {
            // Confirma a saída e volta à tela dos salões
            errorPrevention.visibility = View.GONE
            outsideCard.visibility = View.GONE

            val salonNumber: EditText = findViewById(R.id.salon_creation_number)
            salonNumber.text.clear()
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

    private fun changeLanguage(locale: Locale) {
        val resources = this.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
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