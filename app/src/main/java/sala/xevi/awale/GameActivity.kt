package sala.xevi.awale

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewPropertyAnimator
import android.widget.*
import androidx.annotation.ColorInt
import kotlinx.coroutines.Runnable
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


    private lateinit var binding: ActivityGameBinding

    /**An array of [ImageView] for each box.*/
    private lateinit var boxesIV: Array<ImageView>

    /**Reference for speed animation*/
    private var animationSpeed:Long = 500

    /**When it's true, some controls do nothing (i.ex. player can't move).*/
    private var isAIPlayingOrUIMoving: Boolean = false

    /**Game representation*/
    private lateinit var game: Game

    /**Last game state*/
    private var previousGame: Game? = null

    /**Task doing clock function*/
    private lateinit var  clock: TimerTask

    private var animateMove: ViewPropertyAnimator? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        binding = ActivityGameBinding.inflate(layoutInflater)
        fillBoxesIVArray()
        setContentView(binding.root)

        ///

        binding.undoP1.setOnClickListener { undoLastMov() }
        binding.undoP2.setOnClickListener { if (game.player2.level == Player.Levels.HUMAN) undoLastMov() else redoMachineMov() }

        binding.playAgainBtn.setOnClickListener{playAgain()}

        for (box in boxesIV){
            box.setOnClickListener { v-> boxClicked(v) }
            box.setOnLongClickListener { longClickedBox(  boxesIV.indexOf(box) )}
        }

        if (savedInstanceState == null) {
            animationSpeed = intent.getIntExtra(SPEED_ANIMATION, 500).toLong()
            game = intent.getParcelableExtra<Game>(GAME)!!
            binding.player1Background.backgroundTintList =intent.getParcelableExtra(BACKGROUND_BOARD)
            binding.player2Background.backgroundTintList =intent.getParcelableExtra(BACKGROUND_BOARD)

            binding.namePlayer1ET.text = game.player1.name
            binding.namePlayer2ET.text = game.player2.name

            /*
            val c = Color.parseColor("#30"+Integer.toHexString(binding.player2Background.backgroundTintList!!.defaultColor).substring(2))
            binding.constraintLayout.backgroundTintList =  ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled)), intArrayOf(c))
            */

            checkUndoPlayer2Image()
            updateIVBoxes()
            updateScores()
            updatePlayerInBold()

            if (game.player1.timeLeft != Int.MAX_VALUE){
                clock = timerTask {updateTimer() }
                Timer().schedule(clock,1000,1000 )
            }

            if (game.player2.level != Player.Levels.HUMAN && !game.isPlayer1Active()){
                callAwePlayer()
            }

            //tests
            //game.boxes = intArrayOf(2,2,2,2,2,2,3,3,3,3,6,2)
            //updateIVBoxes()
        }



    }


    /**
     * Starts a thread for AI movement. When AI returns his movement, calls[Game.playBox] and [animateUpdateBoard]
     */
    private fun callAwePlayer () {
        isAIPlayingOrUIMoving = true
        Thread(Runnable {
            val boxToPlay = AwePlayer.play(game,game.activePlayer.level.ordinal -1)
            runOnUiThread {
                game.playBox(boxToPlay)
                animateUpdateBoard(boxToPlay, game.lastStateBoxs[boxToPlay])
            }
        }).start()
    }


    /**
     * Called when playAgainBtn is pressed (Appears when the game is over).
     * Reload
     */
    private fun playAgain () {
        val newGame = Game(Player(game.player1.name), Player(game.player2.name))
        newGame.player2.level = game.player2.level
        newGame.player1.timeLeft = intent.getIntExtra(TIME_GAME, Int.MAX_VALUE)
        newGame.player2.timeLeft = intent.getIntExtra(TIME_GAME, Int.MAX_VALUE)
        game = newGame
        if (game.player1.timeLeft != Int.MAX_VALUE) {
            clock = timerTask {updateTimer() }
            Timer().schedule(clock,1000,1000 )
        }
        binding.gameOverCV.visibility = View.GONE
        checkUndoPlayer2Image()
        updateIVBoxes()
        updateScores()
        updatePlayerInBold()

        isAIPlayingOrUIMoving = false
        if (!game.isPlayer1Active() && game.player2.level != Player.Levels.HUMAN){
            callAwePlayer()
        }

    }

    /**
     * Called every 1 second if [clock] is running. Updates the times and checks if time is out (and game is over).
     */
    private fun updateTimer(){
        runOnUiThread {
            game.activePlayer.timeLeft--
            binding.player1Timer.text = String.format("%02d:%02d", game.player1.timeLeft/60, game.player1.timeLeft%60)
            binding.player2Timer.text = String.format("%02d:%02d", game.player2.timeLeft/60, game.player2.timeLeft%60)
            if(game.isGameFinished()) showMessageFinishedGame()
        }
    }

    /**
     * Called when long click is done in a box.
     * Shows the number of seeds from the long clicked box.
     * @param boxNumber Is the long clicked box.
     * @return true always.
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
     * @param v Is the clicked box.
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
     * Shows a message when the game is finished.
     * Stops the clock.
     */
    private fun showMessageFinishedGame() {

        isAIPlayingOrUIMoving = true
        clock.cancel()
        if (game.player2.timeLeft<0 || game.player1.score > game.player2.score)  {
            binding.gameOverTV.text = getString(R.string.finished_game, game.player1.name)
        } else if (game.player1.timeLeft < 0 || game.player2.score > game.player1.score) {
            binding.gameOverTV.text = getString(R.string.finished_game, game.player1.name)
        } else {
            binding.gameOverTV.text = getString(R.string.finsished_game_tie)
        }

        if (game.player2.timeLeft <0 || game.player1.timeLeft < 0) {
            binding.gameOverTV.text = getString(R.string.time_over) + System.lineSeparator() + binding.gameOverTV.text.toString()
        }

        binding.gameOverCV.visibility = View.VISIBLE

    }


    /**
     * Updates the name of the player, in bold the active player.
     */
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
        if (animateMove!= null) animateMove!!.cancel()
        outState.run {
            putParcelable(GAME, game)
            putLong(SPEED_ANIMATION, animationSpeed)
            putParcelable(BACKGROUND_BOARD, binding.player2Background.backgroundTintList)
        }

        if (game.player1.timeLeft != Int.MAX_VALUE){
            clock.cancel()
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        // Restore state members from saved instance
        savedInstanceState.run {
            animationSpeed = getLong(SPEED_ANIMATION)
            game = getParcelable(GAME)!!
            binding.namePlayer1ET.text = game.player1.name
            binding.namePlayer2ET.text = game.player2.name
            binding.player1Background.backgroundTintList = getParcelable(BACKGROUND_BOARD)!!
            binding.player2Background .backgroundTintList = binding.player1Background.backgroundTintList


        }
        if (game.player1.timeLeft != Int.MAX_VALUE){
            clock = timerTask {updateTimer() }
            Timer().schedule(clock,1000,1000 )
        }
        checkUndoPlayer2Image()
        updateIVBoxes()
        updateScores()
        updatePlayerInBold()
        /*if (game.player1.timeLeft != Int.MAX_VALUE){
            binding.player1Timer.visibility = View.VISIBLE
            binding.player2Timer.visibility = View.VISIBLE
        }*/
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, Intent().putExtra(GAME, if(!game.isGameFinished()) game as Parcelable else null ))
        finish()
    }

    /**
     * Checks if player2 undo button will be refresh (AI) or undo (Human) and
     * if player2Layout is rotated(Human) or not(AI).
     */
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
     * Recursive function for sowing animation. If the player reaps, calls [animateReap] at last. If not, calls [animateFinished]
     * @param origin The origin of the movement, where the player pick up their seeds.
     * @param position The box position of the animation.
     * @param pendingPositions The pending boxes to be animated.
     * @param firstMov true if the animation is on the first movement. The box will be without seeds at the first animation/image change.
     * @return The animation, for next function.
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
                }

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
     * When finishes calls [animateFinished]
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

    /**
     * Called when [animateSowing] or [animateReap] (if has been called) is finished.
     * Calls [updateScores] and [updatePlayerInBold], restores the boxes background and
     * if AI is playing, calls a thread for AI movement and plays it.
     * Finally, turns [isAIPlayingOrUIMoving] false and checks if game is finished.
     * If game is finished, calls [showMessageFinishedGame].
     */
    private fun animateFinished (){
        updateScores()
        updatePlayerInBold()
        for (i in 0..11){
            boxesIV[i].background = null
        }
        if (game.activePlayer.level != Player.Levels.HUMAN){
            callAwePlayer()
        } else {
            isAIPlayingOrUIMoving = false
        }
        if (game.isGameFinished()) {
            updateScores()
            showMessageFinishedGame()
        }
    }

    /**
     * After a valid movement, this function is called to animate the change of the game. First
     * puts background on the related boxes and starts the animation with [animateSowing].
     * @param start the first box to change.
     * @param total The number of boxes to be changed.
     */
    private fun animateUpdateBoard(start: Int, total: Int) {
        for (i in 0..total){
            boxesIV[(start+i)%12].background = getDrawable(R.drawable.box_0)
        }
        animateMove = animateSowing (start, start, total, true)
        animateMove!!.start()
    }

    /**
     * Undoes last movement.
     */
    private fun undoLastMov() {

        if (previousGame == null || isAIPlayingOrUIMoving) { //previousGame == null if the game is at the start.
            return
        }
        val player2level = game.player2.level
        game = previousGame!!
        game.player2.level = player2level
        updateUIGame()

    }

    /**
     * Redoes the machine movement.
     */
    private fun redoMachineMov(){
        if (AwePlayer.lastMov == null || isAIPlayingOrUIMoving || game.player2.level == Player.Levels.HUMAN) return

        game.undoLastMov()
        updateUIGame()
        game.playBox(AwePlayer.lastMov!!)
        animateUpdateBoard(AwePlayer.lastMov!!, game.lastStateBoxs[AwePlayer.lastMov!!])
    }

    /**
     * Updates boxes, scores and player in bold calling [updateIVBoxes], [updateScores] and [updatePlayerInBold].
     */
    private fun updateUIGame() {
        updateIVBoxes()
        updateScores()
        updatePlayerInBold()
    }


}