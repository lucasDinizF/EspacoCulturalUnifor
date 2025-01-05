package com.example.espacocultural

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.espacocultural.models.Artists
import com.example.espacocultural.models.Arts
import com.example.espacocultural.models.GlobalVariables
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView

class ArtistsAdapter(
    private val artistsList: MutableList<Artists>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ArtistsAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onDeleteIconClick(position: Int)
        fun onEditIconClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.artists_recycler_view, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentArtists = artistsList[position]

        holder.bind(currentArtists, position)
    }

    override fun getItemCount(): Int {
        return artistsList.size
    }

    fun removeItem(position: Int) {
        artistsList.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.artist_name)
        val artistBiography: TextView = itemView.findViewById(R.id.artist_biography)
        val imageView: ShapeableImageView = itemView.findViewById(R.id.artist_image)
        val artistNumberButton: Button = itemView.findViewById(R.id.artist_button)
        val deleteIcon: ImageView = itemView.findViewById(R.id.delete_artist) // Adiciona as ImageViews com as opções
        val editIcon: ImageView = itemView.findViewById(R.id.edit_artist) // Adiciona as ImageViews com as opções

        fun bind(artists: Artists, position: Int) {
            nameTextView.text = artists.name
            artistBiography.text = artists.biography
            imageView.setImageDrawable(artists.image)

            // Definindo a visibilidade das ImageViews com base na variável showOptions
            if (artists.showOptions) {
                deleteIcon.visibility = View.VISIBLE
                editIcon.visibility = View.VISIBLE
            } else {
                deleteIcon.visibility = View.GONE
                editIcon.visibility = View.GONE
            }

            // Definir um listener de clique para o botão
            artistNumberButton.setOnClickListener {
                // Abrir a tela de ArtistInfoPage, passando o identificador único do artista como parâmetro extra
                val intent = Intent(itemView.context, ArtistInfoPage::class.java)
                intent.putExtra("artistId", artists.name)
                itemView.context.startActivity(intent)
                GlobalVariables.lastPage = HomePage::class.java

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