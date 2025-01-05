package com.example.espacocultural

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.espacocultural.models.GlobalVariables
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import java.util.Locale

class QrPage : AppCompatActivity() {

    private var isCameraActive = true
    private lateinit var barcodeView: DecoratedBarcodeView
    private val CAMERA_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar o idioma
        when (GlobalVariables.appLanguage) {
            "pt" -> changeLanguage(Locale("pt"))
            "en" -> changeLanguage(Locale("en"))
            else -> changeLanguage(Locale("es"))
        }

        enableEdgeToEdge()
        setContentView(R.layout.qr_page)

        // Inicializa a visualização da câmera
        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        barcodeView.decodeContinuous(callback)

        checkCameraPermission()

        // Botão ativar/desativar câmera
        val toggleCamera: Button = findViewById(R.id.toggle_camera_button)
        toggleCamera.setOnClickListener {
            toggleCamera(toggleCamera)
        }

        // Botões barra inferior
        val homeButton = findViewById<Button>(R.id.homeButton)
        val compassButton = findViewById<Button>(R.id.compassButton)
        val settingsButton = findViewById<Button>(R.id.settingsButton)

        homeButton.setOnClickListener {
            changeScreen(this, HomePage::class.java)
        }

        compassButton.setOnClickListener {
            changeScreen(this, SalonsPage::class.java)
        }

        settingsButton.setOnClickListener {
            changeScreen(this, SettingsPage::class.java)
        }
    }

    private fun changeScreen(activity: Activity, clasS: Class<*>?) {
        GlobalVariables.lastPage = activity::class.java
        val intent = Intent(activity, clasS)
        startActivity(intent)
        activity.finish()
        activity.overridePendingTransition(0, 0)
    }

    private val callback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            result?.let {
                val qrCodeContent = it.text
                val parts = qrCodeContent.split(":")
                if (parts.size == 2) {
                    val salonIdString = parts[0]
                    val artId = parts[1]

                    try {
                        val salonId = salonIdString.toInt()

                        // Use salonId e artId conforme necessário
                        val intent = Intent(this@QrPage, ArtInfoPage::class.java)
                        intent.putExtra("salonId", salonId)
                        intent.putExtra("artId", artId)
                        startActivity(intent)
                    } catch (e: NumberFormatException) {
                        // Trate o erro caso salonId não seja um número válido
                        e.printStackTrace()
                    }
                }
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
            // Se necessário, lide com os pontos de resultado possível aqui
        }
    }

    override fun onResume() {
        super.onResume()
        // Inicia a câmera quando a atividade é retomada
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        // Pausa a câmera quando a atividade é pausada para liberar recursos
        barcodeView.pause()
    }

    private fun toggleCamera(button: Button) {
        if (isCameraActive) {
            barcodeView.pause()
            val cameraContainer: FrameLayout = findViewById(R.id.camera_preview)
            cameraContainer.removeView(barcodeView)
            button.setText(R.string.camera_on)
        } else {
            val cameraContainer: FrameLayout = findViewById(R.id.camera_preview)
            cameraContainer.addView(
                barcodeView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            barcodeView.resume()
            button.setText(R.string.camera_off)
        }
        isCameraActive = !isCameraActive
    }

    private fun changeLanguage(locale: Locale) {
        val resources = this.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Solicita permissão
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            // A permissão já foi concedida, inicie a câmera
            initCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, inicie a câmera
                initCamera()
            } else {
                // Permissão negada, informe ao usuário
                Toast.makeText(this, "Permissão da câmera negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initCamera() {
        // Inicia a câmera quando a atividade é criada
        barcodeView.resume()
    }
}
