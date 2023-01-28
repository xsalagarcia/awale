package sala.xevi.awale

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import sala.xevi.awale.databinding.ActivityMainBinding
import sala.xevi.awale.models.Game
import sala.xevi.awale.models.Player
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**A game representation*/
    private var game: Game? = null

    /**An object extending [ActivityResultLauncher<intent>] for receiving data from ActivityGame*/
    private val startForResult = registerForActivityResult( ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        //https://stackoverflow.com/questions/61455381/how-to-replace-startactivityforresult-with-activity-result-apis
        //this must be called out of playPressed function.
        if (result.resultCode == Activity.RESULT_OK) {
            game = result.data!!.getParcelableExtra(GAME)
            if (game != null) {
                binding.resumeBtn.visibility = View.VISIBLE
                binding.resumeSpc.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()


        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)


        binding.player2Spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.array_levels))

        //listeners
        binding.blueSB.setOnSeekBarChangeListener(seekBarListener)
        binding.greenSB.setOnSeekBarChangeListener(seekBarListener)
        binding.redSB.setOnSeekBarChangeListener(seekBarListener)
        binding.startBtn.setOnClickListener { playPressed() }
        binding.resumeBtn.setOnClickListener { resumePressed() }
        binding.saveBtn.setOnClickListener { saveGamePressed() }
        binding.loadBtn.setOnClickListener { loadGamePressed() }
        binding.customBoardSW.setOnCheckedChangeListener { _, isChecked -> binding.background.visibility = if (isChecked) View.VISIBLE else View.GONE }
        binding.playWithTimeSW.setOnCheckedChangeListener { _, isChecked ->
            binding.minutesET.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.minutesTV.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.minutesET.filters= (arrayOf(object: InputFilter {//MAX 999 MINUTES
            override fun filter(source: CharSequence?,start: Int,end: Int,dest: Spanned?,dstart: Int,dend: Int): CharSequence {
                return if (Integer.parseInt(dest.toString() + source.toString()) > 1000)  "" else source!!
            }
        }))

        if (savedInstanceState == null) {
            binding.redSB.progress = getDefaultSharedPreferences(this).getInt(RED_SLIDER, 175)
            binding.greenSB.progress = getDefaultSharedPreferences(this).getInt(GREEN_SLIDER, 115)
            binding.blueSB.progress = getDefaultSharedPreferences(this).getInt(BLUE_SLIDER, 24)
            binding.playWithTimeSW.isChecked = getDefaultSharedPreferences(this).getBoolean(IS_TIME_ACTIVE, false)
            binding.customBoardSW.isChecked = getDefaultSharedPreferences(this).getBoolean(CUSTOM_BOARD_ACTIVE, false)
            binding.animationBar.progress = getDefaultSharedPreferences(this).getInt(SPEED_ANIMATION, 500)
            binding.minutesET.setText(getDefaultSharedPreferences(this).getInt(TIME_GAME, 10).toString())
            binding.player2Spinner.setSelection(getDefaultSharedPreferences(this).getInt(PLAYER2_LEVEL, 0))
        }
    }

    /**
     * Called when [ActivityMainBinding.startBtn] is clicked. Sets the game with selected player2 level,
     * selected time if it's active and calls [launchGameActivity].
      */
    private fun playPressed() {
        game = Game(Player(getString(R.string.default_player1)), Player(getString(R.string.default_player2)))
        game!!.player2.level = Player.Levels.values()[binding.player2Spinner.selectedItemPosition]
        if (binding.playWithTimeSW.isChecked ) {
            game!!.player1.timeLeft = Integer.parseInt(binding.minutesET.text.toString())*60
            game!!.player2.timeLeft = game!!.player1.timeLeft
        }
       launchGameActivity()
    }

    /**
     * Called when [ActivityMainBinding.resumeBtn] is clicked. If there is a game, will continue it.
     * But with the [ActivityMainBinding.player2Spinner] level in [Game.player2]
     */
    private fun resumePressed() {
        if (game == null) return
        game!!.player2.level = Player.Levels.values()[binding.player2Spinner.selectedItemPosition]
        launchGameActivity()
    }

    /**
     * Launches GameActivity. game hasn't to be null.
     * Called from [loadGamePressed], [resumePressed] or [playPressed].
     * Puts game, animation and time selected to [Intent].
     */
    private fun launchGameActivity(){
        val intent = Intent(this, GameActivity::class.java)

        intent.putExtra(GAME, game as Parcelable)

        intent.putExtra(SPEED_ANIMATION, binding.animationBar.progress)

        intent.putExtra(TIME_GAME, if (binding.playWithTimeSW.isChecked) Integer.parseInt(binding.minutesET.text.toString())*60 else Int.MAX_VALUE)

        intent.putExtra(BACKGROUND_BOARD,
            if (binding.customBoardSW.isChecked) binding.background.backgroundTintList
            else ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(getColor(R.color.default_board))))

        startForResult.launch(intent)
    }

    /**
     * Saves the current [game], if exists.
     */
    private fun saveGamePressed() {
        if (game == null) return
        baseContext.openFileOutput(SAVED_GAME, Context.MODE_PRIVATE).use {
            val oos = ObjectOutputStream(it)
            try {
                oos.writeObject(LocalDateTime.now())
                oos.writeObject(game)
                oos.close()
                Toast.makeText(this, getString(R.string.saved_game), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.couldnt_save_game), Toast.LENGTH_SHORT).show()
            }
                it.close()
        }
    }

    /**
     * Loads a saved [Game].
     */
    private fun loadGamePressed() {
        try {
            baseContext.openFileInput(SAVED_GAME).use {

                val ois = ObjectInputStream(it)
                val ldt = ois.readObject() as LocalDateTime
                val g = ois.readObject() as Game
                ois.close()
                it.close()
                AlertDialog.Builder(this).apply {
                    setPositiveButton(getString(R.string.ok)) { _, _ -> game = g; resumePressed() }
                    setNegativeButton(getString(R.string.cancel), null)
                    setTitle(R.string.continue_this_game)
                    setMessage(getString( R.string.saved_at) + ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")))
                }.show()
            }
        } catch ( e:  FileNotFoundException) {
            Toast.makeText(this, getString(R.string.no_saved_game), Toast.LENGTH_LONG).show()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Saves the status of the activity
        outState.run {
            putParcelable(GAME, game)
            putBoolean(IS_TIME_ACTIVE, binding.playWithTimeSW.isChecked)
            putBoolean(CUSTOM_BOARD_ACTIVE, binding.customBoardSW.isChecked)
            putInt(RED_SLIDER, binding.redSB.progress)
            putInt(BLUE_SLIDER, binding.blueSB.progress)
            putInt(GREEN_SLIDER, binding.greenSB.progress)
            putInt(MINUTES, Integer.parseInt ( binding.minutesET.text.toString() ))
            putInt(PLAYER2_LEVEL, binding.player2Spinner.selectedItemPosition)
            putInt(SPEED_ANIMATION, binding.animationBar.progress)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState)

        // Restore state members from saved instance
        savedInstanceState.run {
            game = getParcelable(GAME)
            binding.playWithTimeSW.isChecked = getBoolean(IS_TIME_ACTIVE)
            binding.customBoardSW.isChecked = getBoolean(CUSTOM_BOARD_ACTIVE)
            binding.redSB.progress = getInt(RED_SLIDER)
            binding.blueSB.progress = getInt(BLUE_SLIDER)
            binding.greenSB.progress = getInt(GREEN_SLIDER)
            binding.minutesET.setText(getInt(MINUTES).toString())
            binding.player2Spinner.setSelection(getInt(PLAYER2_LEVEL))
            binding.animationBar.progress = getInt(SPEED_ANIMATION)
        }

    }



    override fun onStop() {
        getDefaultSharedPreferences(this).edit().apply {
            putInt(SPEED_ANIMATION, binding.animationBar.progress)
            putInt(RED_SLIDER, binding.redSB.progress)
            putInt(GREEN_SLIDER, binding.greenSB.progress)
            putInt(BLUE_SLIDER, binding.blueSB.progress)
            putInt(TIME_GAME, binding.minutesET.text.toString().toInt())
            putBoolean(IS_TIME_ACTIVE, binding.playWithTimeSW.isChecked)
            putBoolean(CUSTOM_BOARD_ACTIVE, binding.customBoardSW.isChecked)
            putInt(PLAYER2_LEVEL, binding.player2Spinner.selectedItemPosition)
            apply()
        }
        super.onStop()
    }

    /**A SeekBarListener for color selection*/
    private val seekBarListener = object: SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val newColor = "#" + String.format("%02X", binding.redSB.progress) + String.format("%02X", binding.greenSB.progress) + String.format("%02X", binding.blueSB.progress)
            //https://stackoverflow.com/questions/15543186/how-do-i-create-colorstatelist-programmatically
            binding.background.backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(Color.parseColor(newColor)))
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            //do nothing
        }
        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            //do nothing
        }
    }


}