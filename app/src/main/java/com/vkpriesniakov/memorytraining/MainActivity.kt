package com.vkpriesniakov.memorytraining

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.vkpriesniakov.memorytraining.adapters.MemoryBoardAdapter
import com.vkpriesniakov.memorytraining.databinding.ActivityMainBinding
import com.vkpriesniakov.memorytraining.models.BoardSize
import com.vkpriesniakov.memorytraining.models.MemoryGame
import com.vkpriesniakov.memorytraining.models.UserImageList
import com.vkpriesniakov.memorytraining.utils.EXTRA_BOARD_SIZE
import com.vkpriesniakov.memorytraining.utils.EXTRA_GAME_NAME

class MainActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "MainActivityTag"
        private const val CREATE_REQUEST_CODE = 39701
    }

    private val db = Firebase.firestore
    private var gameName:String? = null
    private var customGameImages:List<String>? = null
    private lateinit var mAdapter: MemoryBoardAdapter
    private lateinit var bdn: ActivityMainBinding
    private lateinit var memoryGame: MemoryGame
    private var boardSize:BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bdn = ActivityMainBinding.inflate(layoutInflater)
        val view = bdn.root
        setContentView(view)
        
        setupBoard()

    }

    private fun updateGameWithFlip(position: Int) {
        //Error handeling
        if (memoryGame.haveWonGame()){
            Snackbar.make(bdn.clRoot, "You already won!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)){
            Snackbar.make(bdn.clRoot, "Invalid move!", Snackbar.LENGTH_SHORT).show()
            //Invalid move alert
            return
        }
       if(memoryGame.flipCard(position)){
           Log.i(TAG, "Found match! Num pairs found: ${memoryGame.numPairsFound}")
           val color = ArgbEvaluator().evaluate(
                   memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                   ContextCompat.getColor(this, R.color.color_progers_none),
                   ContextCompat.getColor(this, R.color.color_progers_full)
           ) as  Int
           bdn.txtPairsNum.setTextColor(color)
           val pairsTxt = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
        bdn.txtPairsNum.text = pairsTxt
           if (memoryGame.haveWonGame()){
               Snackbar.make(bdn.recyclerViewBoard, "You won! Congratz.", Snackbar.LENGTH_LONG).show()
               CommonConfetti.rainingConfetti(bdn.clRoot, intArrayOf(Color.YELLOW, Color.GREEN, Color.MAGENTA)).oneShot()
           }
       }
        val numMovesTxt = "Moves: ${memoryGame.getNumMoves()}"
        bdn.txtMovesLeft.text = numMovesTxt
        mAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.mi_refresh ->{
                //setup the game again
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game", null, View.OnClickListener { setupBoard() })
                } else{
                    setupBoard()
                }
                return true
            }
            R.id.mi_new_size ->{
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom_game ->{
                showCreationDialog()
                return true
            }
            R.id.mi_download ->{
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null){
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
            Log.e(TAG, "Custom Game name is $customGameName in ActivityResult")
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            //Grab the text of the game name that user wants to download
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.ed_id_game_to_download)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    private fun downloadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null){
                Log.e(TAG, "Invalid data from Firestore")
                Snackbar.make(bdn.clRoot, "Sorry, we couldn't find any such game, $customGameName", Snackbar.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val numCards = userImageList.images.size *2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            for (imageUrl in userImageList.images){
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(bdn.clRoot, "You're now playing $customGameName!", Snackbar.LENGTH_SHORT).show()
            gameName = customGameName
            setupBoard()
            Log.e(TAG, "Game name is $gameName in downloadGame")
        }.addOnFailureListener{ exception ->
            Log.e(TAG, "Exception on retrieving game", exception)
        }

    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.rb_group)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            //Set value for the board size
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rb_easy -> BoardSize.EASY
                R.id.rb_medium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate to a new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize )
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.rb_group)
        when (boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rb_easy)
            BoardSize.MEDIUM ->radioGroupSize.check(R.id.rb_medium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rb_hard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            //Set value for the board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rb_easy -> BoardSize.EASY
                R.id.rb_medium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        })
    }

    private fun showAlertDialog(mTitle:String, view: View?, positiveClickListener:View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(mTitle)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok"){_,_ ->
                    positiveClickListener.onClick(null)
                }.show()
    }

    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize){
            BoardSize.EASY -> {
                bdn.txtMovesLeft.text = "Easy: 4 x 2"
                bdn.txtPairsNum.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                bdn.txtMovesLeft.text = "Easy: 6 x 3"
                bdn.txtPairsNum.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                bdn.txtMovesLeft.text = "Easy: 6 x 4"
                bdn.txtPairsNum.text = "Pairs: 0 / 12"
            }
        }
        bdn.txtPairsNum.setTextColor(ContextCompat.getColor(this, R.color.color_progers_none))
        memoryGame = MemoryGame(boardSize, customGameImages)

        mAdapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object : MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                Log.i(TAG, "Card clicked $position")
                updateGameWithFlip(position)
            }
        })
        bdn.recyclerViewBoard.adapter = mAdapter
        bdn.recyclerViewBoard.setHasFixedSize(true)
        bdn.recyclerViewBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }
}