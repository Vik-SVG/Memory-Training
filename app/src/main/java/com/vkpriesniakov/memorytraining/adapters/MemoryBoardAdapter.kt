package com.vkpriesniakov.memorytraining.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.squareup.picasso.Picasso
import com.vkpriesniakov.memorytraining.R
import com.vkpriesniakov.memorytraining.models.BoardSize
import com.vkpriesniakov.memorytraining.models.MemoryCard
import kotlin.math.min

class MemoryBoardAdapter(private val mContext: Context,
                         private val boardSize: BoardSize,
                         private val cards: List<MemoryCard>,
                         private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<MemoryBoardAdapter.MemoryHolder>() {


    companion object{
        private const val  MARGIN_SIZE = 10
        private const val TAG = "MemoryAdapterTag"
    }

    interface CardClickListener{
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryHolder {
        val cardWidth = parent.width/boardSize.getWidth()-(2* MARGIN_SIZE)
        val cardHeight = parent.height/boardSize.getHeight()-(2* MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)
        val view =  LayoutInflater.from(mContext).inflate(R.layout.memory_card, parent, false)
        val mLayoutParams = view.findViewById<CardView>(R.id.memory_card).layoutParams as ViewGroup.MarginLayoutParams
        mLayoutParams.width = cardSideLength
        mLayoutParams.height = cardSideLength
        mLayoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return MemoryHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = boardSize.numCards


    inner class MemoryHolder(itemView:View):RecyclerView.ViewHolder(itemView){
        private val imageButton = itemView.findViewById<ImageButton>(R.id.image_button)
        fun bind(position: Int) {
            val memoryCard = cards[position]
            if (memoryCard.isFaceUp){
                if (memoryCard.imageUrs != null){
                    Picasso.get().load(memoryCard.imageUrs).placeholder(R.drawable.ic_image).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else{
                imageButton.setImageResource(R.drawable.ic_background_cards)
            }

            imageButton.alpha = if (memoryCard.isMatch) .4f else 1.0f
            val colorStateList = if (memoryCard.isMatch) ContextCompat.getColorStateList(mContext, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked on position $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
