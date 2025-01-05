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

class ArtsAdapter(
    private val artsList: MutableList<Arts>,
    private val itemClickListener: ArtsAdapter.OnItemClickListener,
    private val salonId: Int
) : RecyclerView.Adapter<ArtsAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onDeleteIconClick(position: Int)
        fun onEditIconClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.arts_recycler_view, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentArt = artsList[position]

        holder.bind(currentArt, position)
    }

    override fun getItemCount(): Int {
        return artsList.size
    }

    fun removeItem(position: Int) {
        artsList.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name)
        val imageView: ShapeableImageView = itemView.findViewById(R.id.image)
        val artNumberButton: RelativeLayout = itemView.findViewById(R.id.art_number_button)
        val deleteIcon: ImageView = itemView.findViewById(R.id.delete_art) // Adiciona as ImageViews com as opções
        val editIcon: ImageView = itemView.findViewById(R.id.edit_art) // Adiciona as ImageViews com as opções

        fun bind(arts: Arts, position: Int) {
            nameTextView.text = arts.name
            imageView.setImageDrawable(arts.image)

            // Definindo a visibilidade das ImageViews com base na variável showOptions
            if (arts.showOptions) {
                deleteIcon.visibility = View.VISIBLE
                editIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
                editIcon.visibility = View.GONE
            }

            // Definir um listener de clique para o botão
            artNumberButton.setOnClickListener {
                // Abrir a tela de ArtsPage, passando o identificador único do salão como parâmetro extra
                val intent = Intent(itemView.context, ArtInfoPage::class.java)
                intent.putExtra("artId", arts.name)
                intent.putExtra("salonId", salonId)
                itemView.context.startActivity(intent)
                GlobalVariables.lastPage = ArtsPage::class.java

                // Definir nenhuma animação de transição
                (itemView.context as Activity).overridePendingTransition(0, 0)
            }

            deleteIcon.setOnClickListener {
                // Deleta salão
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onDeleteIconClick(position)
                }
            }

            editIcon.setOnClickListener {
                // Edita salão
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    itemClickListener.onEditIconClick(position)
                }
            }
        }
    }
}