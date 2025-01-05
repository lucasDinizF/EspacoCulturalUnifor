
package com.example.espacocultural

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.espacocultural.models.GlobalVariables
import com.example.espacocultural.models.Salons
import com.google.android.material.imageview.ShapeableImageView

class SalonsAdapter(
    private val salonsList: MutableList<Salons>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<SalonsAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onDeleteIconClick(position: Int)
        fun onEditIconClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.salons_recycler_view, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSalon = salonsList[position]

        holder.bind(currentSalon, position)
    }

    override fun getItemCount(): Int {
        return salonsList.size
    }

    fun removeItem(position: Int) {
        salonsList.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name)
        val imageView: ShapeableImageView = itemView.findViewById(R.id.image)
        val salonNumberButton: RelativeLayout = itemView.findViewById(R.id.salon_number_button)
        val deleteIcon: ImageView = itemView.findViewById(R.id.delete_salon) // Adiciona as ImageViews com as opções
        val editIcon: ImageView = itemView.findViewById(R.id.edit_salon) // Adiciona as ImageViews com as opções

        fun bind(salons: Salons, position: Int) {
            nameTextView.text = salons.name
            imageView.setImageDrawable(salons.image)

            // Definindo a visibilidade das ImageViews com base na variável showOptions
            if (salons.showOptions) {
                deleteIcon.visibility = View.VISIBLE
                editIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
                editIcon.visibility = View.GONE
            }

            // Definir um listener de clique para o botão
            salonNumberButton.setOnClickListener {
                // Abrir a tela de ArtsPage, passando o identificador único do salão como parâmetro extra
                val intent = Intent(itemView.context, ArtsPage::class.java)
                intent.putExtra("salonId", salons.id)
                itemView.context.startActivity(intent)
                GlobalVariables.lastPage = SalonsPage::class.java

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