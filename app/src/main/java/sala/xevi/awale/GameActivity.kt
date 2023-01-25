package sala.xevi.awale

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.*
import sala.xevi.awale.databinding.ActivityGameBinding
import sala.xevi.awale.exceptions.IllegalMovementException
import sala.xevi.awale.models.AwePlayer
import sala.xevi.awale.models.Game
import sala.xevi.awale.models.Player
import java.util.*
import kotlin.concurrent.timerTask

/**
 * The main activity has the board view representation.
 */
class GameActivity () : AppCompatActivity() {

       //var game: Game = Game(Player("Player 1", 0), Player("Player 2",  0))
    private lateinit var binding: ActivityGameBinding

    private lateinit var boxesIV: Array<ImageView>

    private var animationSpeed:Long = 500

    var isAIPlayingOrUIMoving: Boolean = false;

    private lateinit var game: Game

    private var previousGame: Game? = null;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityGameBinding.inflate(layoutInflater)
        fillBoxesIVArray()
        setContentView(binding.root)



        if (savedInstanceState == null) {
            game = intent.getParcelableExtra<Game>(GAME)!!
            binding.player1Background.backgroundTintList =intent.getParcelableExtra(BACKGROUND_BOARD)
            binding.player2Background.backgroundTintList =intent.getParcelableExtra(BACKGROUND_BOARD)

            binding.namePlayer1ET.text = game.player1.name
            binding.namePlayer2ET.text = game.player2.name

            checkUndoPlayer2Image()
            updateIVBoxes()
            updateScores()
            updatePlayerInBold()

            //tests
            //game.boxes = intArrayOf(2,2,2,2,2,2,3,3,3,3,6,2)
            //updateIVBoxes()
        }

        binding.undoP1.setOnClickListener { undoLastMov() }
        binding.undoP2.setOnClickListener { if (game.player2.level == Player.Levels.HUMAN) undoLastMov() else redoMachineMov() }

        for (box in boxesIV){
            box.setOnClickListener { v-> boxClicked(v) }
            box.setOnLongClickListener { longClickedBox(  boxesIV.indexOf(box) )}
        }






    }

    /**
     * Called when long click is done in a box.
     * Shows the number of seeds from the long clicked box.
     */
    private fun longClickedBox(boxNumber: Int): Boolean {
        binding.seedsNumberInfoTV.text = game.boxes[boxNumber].toString()
        binding.seedsNumberInfo.rotation = if (!game.isPlayer1Active())  180F else 0F
        binding.seedsNumberInfo.visibility = View.VISIBLE
        Timer().schedule(timerTask { runOnUiThread{binding.seedsNumberInfo.visibility = View.GONE} }, 1000)
        return true
    }


    /**
     * Called when a box is clicked.
     * @param v Is the box clicked.
     */
    private fun boxClicked(v: View?) {
        val boxClicked:Int = boxesIV.indexOf(v) //gets the box number that is clicked.
        if (!isAIPlayingOrUIMoving && !game.isGameFinished() &&
            (game.isPlayer1Active() && boxClicked > 5 || !game.isPlayer1Active() && boxClicked < 6) &&
            game.boxes[boxClicked] > 0) {

            try {
                val previous = game.copyMe()
                game.playBox(boxClicked)
                previousGame = previous
                isAIPlayingOrUIMoving = true
                animateUpdateBoard(boxClicked, game.lastStateBoxs[boxClicked])



            } catch (e: IllegalMovementException  ){
                Toast.makeText(this, getString(R.string.illegal_movement), Toast.LENGTH_SHORT).show()
            }
        }


    }

    /**+
     * Shows a messaege when the game is finished.
     */
    private fun showMessageFinishedGame() {

        val winner = if (game.player1.score > game.player2.score)  {
            game.player1.name
        } else {
            game.player2.name
        }
        binding.gameOverTV.text = getString(R.string.finished_game) + winner
        binding.gameOverCV.visibility = View.VISIBLE

    }


    private fun updatePlayerInBold() {
        if (game.isPlayer1Active()){
            binding.namePlayer1ET.setTypeface(null, Typeface.BOLD)
            binding.namePlayer2ET.setTypeface(null, Typeface.NORMAL)
        } else {
            binding.namePlayer2ET.setTypeface(null, Typeface.BOLD)
            binding.namePlayer1ET.setTypeface(null, Typeface.NORMAL)
        }
    }

    /**
     * Puts the ImageView boxes into an Array<Box>
     */
    private fun fillBoxesIVArray (){
        boxesIV = arrayOf(
            binding.box0,
            binding.box1,
            binding.box2,
            binding.box3,
            binding.box4,
            binding.box5,
            binding.box6,
            binding.box7,
            binding.box8,
            binding.box9,
            binding.box10,
            binding.box11,
        )
    }


    override fun onSaveInstanceState(outState: Bundle) {
        // Saves the current game state
        outState?.run {
            putParcelable(GAME, game)
            putParcelable(BACKGROUND_BOARD, binding.player2Background.backgroundTintList)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        // Restore state members from saved instance
        savedInstanceState.run {
            game = getParcelable(GAME)!!
            binding.namePlayer1ET.text = game.player1.name
            binding.namePlayer2ET.text = game.player2.name
            binding.player1Background.backgroundTintList = getParcelable(BACKGROUND_BOARD)!!
            binding.player2Background .backgroundTintList = binding.player1Background.backgroundTintList
        }
        checkUndoPlayer2Image()
        updateIVBoxes()
        updateScores()
        updatePlayerInBold()
    }


    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, Intent().putExtra(GAME, if(!game.isGameFinished()) game as Parcelable else null ))
        finish()
    }


    private fun checkUndoPlayer2Image() {
        if (game.player2.level != Player.Levels.HUMAN) {
            binding.undoP2.setImageDrawable(getDrawable(R.drawable.ic_baseline_refresh_24))
            binding.player2Layout.rotation= 0F
        } else {
            binding.undoP2.setImageDrawable(getDrawable(R.drawable.ic_baseline_undo_24))
            binding.player2Layout.rotation = 180F
        }
    }
    /**
     * Puts an image representing a number of seeds into the ImageView.
     * @param imageView The ImageView where will be put the seeds.
     * @param seedsNumber The number of seeds to be put.
     */
    private fun putImageSeeds (imageView: ImageView, seedsNumber: Int){
        when(seedsNumber){
            0->imageView.setImageResource(R.drawable.box_0)
            1->imageView.setImageResource(R.drawable.box_1)
            2->imageView.setImageResource(R.drawable.box_2)
            3->imageView.setImageResource(R.drawable.box_3)
            4->imageView.setImageResource(R.drawable.box_4)
            5->imageView.setImageResource(R.drawable.box_5)
            6->imageView.setImageResource(R.drawable.box_6)
            7->imageView.setImageResource(R.drawable.box_7)
            8->imageView.setImageResource(R.drawable.box_8)
            9->imageView.setImageResource(R.drawable.box_9)
            10->imageView.setImageResource(R.drawable.box_10)
            11->imageView.setImageResource(R.drawable.box_11)
            12->imageView.setImageResource(R.drawable.box_12)
            13->imageView.setImageResource(R.drawable.box_13)
            14->imageView.setImageResource(R.drawable.box_14)
            15->imageView.setImageResource(R.drawable.box_15)
            16->imageView.setImageResource(R.drawable.box_16)
            17->imageView.setImageResource(R.drawable.box_17)
            18->imageView.setImageResource(R.drawable.box_18)
            19->imageView.setImageResource(R.drawable.box_19)
            20->imageView.setImageResource(R.drawable.box_20)
            21->imageView.setImageResource(R.drawable.box_21)
            22->imageView.setImageResource(R.drawable.box_22)
            23->imageView.setImageResource(R.drawable.box_23)
        }
        if (seedsNumber > 23) imageView.setImageResource(R.drawable.box_23)




    }

    /**
     * Puts the image in agreement with the [game] representation.
     */
    private fun updateIVBoxes(){
        for (i in 0..11){
            putImageSeeds( boxesIV[i], game.boxes[i])
        }
    }

    /**
     * Updates the scores in agreement with the [game] representation.
     */
    private fun updateScores(){
        binding.scorePlayer2TV.text = game.player2.score.toString()
        binding.scorePlayer1TV.text = game.player1.score.toString()
    }

    /**
     * Recursive function for sowing animation.
     * @param origin The origin of the movement, where the player pick up their seeds.
     * @param position The box position of the animation.
     * @param pendingPositions The pending boxes to be animated.
     * @param firstMov true if the animation is on the first movement. The box will be without seeds at the first animation/image change.
     */
    private fun animateSowing (origin: Int, position: Int, pendingPositions: Int, firstMov: Boolean) : ViewPropertyAnimator{
        val ani = boxesIV[position % 12].animate().apply {

            duration = animationSpeed
            scaleXBy(0.2f)
            scaleYBy(0.2f)
        }.withEndAction {
            boxesIV[position % 12].animate().apply {

                if (firstMov) {
                    putImageSeeds(boxesIV[(position)%12], 0)

                    //boxesIV[(position)%12].background = null
                } else {
                    if (origin%12 != position%12) {
                        putImageSeeds(boxesIV[(position) % 12], game.lastStateBoxs[(position) % 12] + 1 + (position-origin)/12)
                    } else {
                        //putImageSeeds(boxesIV[(position) % 12], (position-origin)/12)// Don't do this because of https://www.myriad-online.com/resources/docs/awale/espanol/rules.htm rule 6
                    }

                }
                duration = animationSpeed
                scaleXBy(-0.2f)
                scaleYBy(-0.2f)
                if (pendingPositions >0 || (pendingPositions == 0 && origin%12 == position%12)) {
                    val positionToSubstract = if(position == origin || origin%12-(position)%12 != 0) -1 else  0//
                    animateSowing (origin,position +1, pendingPositions + positionToSubstract, false)
                } //else if (game.reapsLastMov > 0){
                    //animateReap(position, game.reapsLastMov)
                //}

            }.withEndAction {
                if (pendingPositions == 0) {//pendingPositions == 0 is the last animation
                    if (game.reapsLastMov > 0) {
                        animateReap(position, game.reapsLastMov)
                    } else {
                        animateFinished()
                    }
                }

            }
        }
        return ani
    }

    /**
     * recursive function for reap animation. Called when there are seeds to reap, after [animateSowing] finishes.
     * @param position The box to reap.
     * @param total number of the boxes to reap.
     */
    private fun animateReap (position: Int, total:Int){
        boxesIV[position%12].animate().apply{
            duration = animationSpeed
            scaleXBy(0.2f)
            scaleYBy(0.2f)
        }.withEndAction {
            boxesIV[position%12].animate().apply {
                putImageSeeds(boxesIV[position%12], 0)
                duration = animationSpeed
                scaleXBy(-0.2f)
                scaleYBy(-0.2f)
                if (total >1) {
                    animateReap (position -1,total -1)
                } else {
                    animateFinished()
                }

            }
        }
    }

    private fun animateFinished (){
        //Potser es podria crear un fil que mentres la animació és present ja pensi la següent jugada. Iniciant-se abans de cridar la animació
        //una vegada la animació acaba, si ja ha acabat de elaborar tirada, fer-la, i si no, esperar a que acabi. AtomicBoolean, AtomicInteger...
        //https://developer.android.com/guide/background/threading#see-also
        updateScores()
        updatePlayerInBold()
        for (i in 0..11){
            boxesIV[i].background = null
        }
        if (game.activePlayer.level != Player.Levels.HUMAN){
            val boxToPlay = AwePlayer.play(game,game.activePlayer.level.ordinal -1)
            game.playBox(boxToPlay)
            animateUpdateBoard(boxToPlay, game.lastStateBoxs[boxToPlay])
        } else {
            isAIPlayingOrUIMoving = false
        }
        if (game.isGameFinished()) {
            showMessageFinishedGame()
        }
    }

    /**
     * After a valid movement, this function is called to animate the change of the game.
     * @param start the first box to change.
     * @param total The number of boxes to be changed.
     */
    private fun animateUpdateBoard(start: Int, total: Int) {
        //animateSowing (start, start, total, true).setStartDelay(500).start()
        //boxesIV[(position)%12].background = getDrawable(R.drawable.box_0)
        for (i in 0..total){
            boxesIV[(start+i)%12].background = getDrawable(R.drawable.box_0)
        }

        animateSowing (start, start, total, true).start()

    }

    /**
     * Undo last movement.
     */
    private fun undoLastMov() {
        //game.undoLastMov()
        if (previousGame == null || isAIPlayingOrUIMoving) {
            return
        }
        val player2level = game.player2.level
        game = previousGame!!
        game.player2.level = player2level
        updateUIGame()

    }

    private fun redoMachineMov(){
        if (AwePlayer.lastMov == null || isAIPlayingOrUIMoving || game.player2.level == Player.Levels.HUMAN) return
        game.undoLastMov()
        updateUIGame()
        game.playBox(AwePlayer.lastMov!!)
        animateUpdateBoard(AwePlayer.lastMov!!, game.lastStateBoxs[AwePlayer.lastMov!!])
    }

    private fun updateUIGame() {
        updateIVBoxes()
        updateScores()
        updatePlayerInBold()
    }






}