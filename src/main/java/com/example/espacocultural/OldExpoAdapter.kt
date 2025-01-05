package com.example.espacocultural

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.espacocultural.models.Arts
import com.example.espacocultural.models.GlobalVariables
import com.google.android.material.imageview.ShapeableImageView

class OldExpoAdapter(
    private val artsList: MutableList<Arts>,
) : RecyclerView.Adapter<OldExpoAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.old_expo_recycler_view, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentArt = artsList[position]

        holder.bind(currentArt, position)
    }

    override fun getItemCount(): Int {
        return artsList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name)
        val imageView: ShapeableImageView = itemView.findViewById(R.id.image)
        val artNumberButton: RelativeLayout = itemView.findViewById(R.id.art_number_button)

        fun bind(arts: Arts, position: Int) {
            nameTextView.text = arts.name
            imageView.setImageDrawable(arts.image)

            // Definir um listener de clique para o botão
            artNumberButton.setOnClickListener {
                // Abrir a tela de ArtsPage, passando o identificador único do salão como parâmetro extra
                val intent = Intent(itemView.context, ArtInfoPage::class.java)
                intent.putExtra("artId", arts.name)
                itemView.context.startActivity(intent)
                GlobalVariables.lastPage = OldExpositionsPage::class.java

                // Definir nenhuma animação de transição
                (itemView.context as Activity).overridePendingTransition(0, 0)
            }
        }
    }
}