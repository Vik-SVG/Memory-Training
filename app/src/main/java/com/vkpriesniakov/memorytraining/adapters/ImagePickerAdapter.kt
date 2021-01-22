package com.vkpriesniakov.memorytraining.adapters

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.vkpriesniakov.memorytraining.R
import com.vkpriesniakov.memorytraining.models.BoardSize
import kotlin.math.max
import kotlin.math.min

class ImagePickerAdapter(private val context: Context,
                         private val chosenImageUris: List<Uri>,
                         private val boardSize: BoardSize,
                         private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ImagePickerViewHolder>() {

    interface  ImageClickListener {
        fun onPlaceholderClicked()
    }

    inner class ImagePickerViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.iv_custom_image)

        fun bind() {
            ivCustomImage.setOnClickListener{
                imageClickListener.onPlaceholderClicked()
            }
        }

        fun bind(uri:Uri) {
            ivCustomImage.setImageURI(uri)
            ivCustomImage.setOnClickListener(null)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePickerViewHolder {
       val view =  LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParams = view.findViewById<ImageView>(R.id.iv_custom_image).layoutParams

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            layoutParams.width = cardHeight + 100
            layoutParams.height = cardHeight + 100
        } else{
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        }
        return ImagePickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImagePickerViewHolder, position: Int) {
        if (position < chosenImageUris.size){
            holder.bind(chosenImageUris[position])
        } else {
            holder.bind()
        }
    }

    override fun getItemCount():Int = boardSize.getNumPairs()
}
