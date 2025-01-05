package com.example.espacocultural

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.espacocultural.models.GlobalVariables
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class AdmLoginPage : AppCompatActivity() {

    var passwordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        when (GlobalVariables.appLanguage) {
            "pt" -> changeLanguage(Locale("pt"))
            "en" -> changeLanguage(Locale("en"))
            else -> changeLanguage(Locale("es"))
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.adm_login_page)

        // Botões da barra superior
        val returnButton = findViewById<Button>(R.id.return_button)

        returnButton.setOnClickListener {
            changeScreen(this, SettingsPage::class.java)
        }

        // Botões da tela
        val login: EditText = findViewById(R.id.login)
        val password: EditText = findViewById(R.id.password)
        val showPassword: TextView = findViewById(R.id.show_password_button)
        val access: Button = findViewById(R.id.access_button)

        showPassword.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG)
        showPassword.setOnClickListener {
            togglePasswordView(password)
        }

        // Banco de dados
        val db = FirebaseFirestore.getInstance()
        val admsRef = db.collection("adms")

        access.setOnClickListener {

            admsRef.whereEqualTo("login", login.text.toString())
                .whereEqualTo("senha", password.text.toString())
                .get()
                .addOnSuccessListener {
                    if (it.isEmpty) {
                        Toast.makeText(this, "Nome de usuário ou senha incorretos", Toast.LENGTH_SHORT).show()
                    } else {
                        // Admin encontrado
                        Toast.makeText(this, "Bem-vindo(a), ${login.text}!", Toast.LENGTH_SHORT).show()
                        GlobalVariables.isAdmin = true
                        changeScreen(this, SettingsPage::class.java)
                    }
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

    fun togglePasswordView(view: EditText) {
        if (!passwordVisible) {
            view.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            passwordVisible = !passwordVisible
        } else {
            view.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordVisible = !passwordVisible
        }
    }

    private fun changeLanguage(locale: Locale) {
        val resources = this.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}