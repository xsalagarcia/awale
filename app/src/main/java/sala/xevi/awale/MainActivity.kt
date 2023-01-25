package sala.xevi.awale

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.ColorStateListDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.DragEvent
import android.view.View

import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
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
    private var game: Game? = null

    //https://stackoverflow.com/questions/61455381/how-to-replace-startactivityforresult-with-activity-result-apis
    //this must be called out of playPressed function.
    private val startForResult = registerForActivityResult( ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            game = result.data!!.getParcelableExtra<Game>(GAME)
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


        //https://stackoverflow.com/questions/15543186/how-do-i-create-colorstatelist-programmatically
        binding.background.backgroundTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled)), intArrayOf(Color.parseColor("#AF7319")))

        binding.player2Spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.array_levels))

        binding.blueSB.setOnSeekBarChangeListener(seekBarListener)
        binding.greenSB.setOnSeekBarChangeListener(seekBarListener)
        binding.redSB.setOnSeekBarChangeListener(seekBarListener)

        binding.startBtn.setOnClickListener { playPressed() }

        binding.resumeBtn.setOnClickListener { resumePressed() }

        binding.saveBtn.setOnClickListener { saveGamePressed() }
        binding.loadBtn.setOnClickListener { loadGamePressed() }

        binding.customBoardSW.setOnCheckedChangeListener { buttonView, isChecked -> binding.background.visibility = if (isChecked) View.VISIBLE else View.GONE }

        if (savedInstanceState == null) {
            binding.playWithTimeSW.setOnCheckedChangeListener { buttonView, isChecked ->
                binding.minutesET.visibility = if (isChecked) View.VISIBLE else View.GONE
                binding.minutesTV.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        } else {

        }




        setColorSliders(getDefaultSharedPreferences(this).getString(BACKGROUND_BOARD, "#AF7319")!!)

        binding.minutesET.setText(getDefaultSharedPreferences(this).getInt(TIME_GAME, 10).toString())



        //AlertDialog amb numberPicker(s)
    }



    private fun playPressed() {
        game = Game(Player(getString(R.string.default_player1)), Player(getString(R.string.default_player2)))
        game!!.player2.level = Player.Levels.values()[binding.player2Spinner.selectedItemPosition]
        if (binding.playWithTimeSW.isChecked ) {
            game!!.player1.timeLeft = Integer.parseInt(binding.minutesET.text.toString())*60
            game!!.player2.timeLeft = game!!.player1.timeLeft
        }
       launchGameActivity()
    }

    private fun resumePressed() {
        if (game== null) return
        game!!.player2.level = Player.Levels.values()[binding.player2Spinner.selectedItemPosition]
        launchGameActivity()
    }

    /**
     * Launches GameActivity. game hasn't to be null.
     * Called from [loadGamePressed], [resumePressed] or [playPressed].
     */
    private fun launchGameActivity(){
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(GAME, game as Parcelable)
        intent.putExtra(BACKGROUND_BOARD, binding.background.backgroundTintList)
        startForResult.launch(intent)
    }

    private fun saveGamePressed() {
        if (game == null) return
        baseContext.openFileOutput(SAVED_GAME, Context.MODE_PRIVATE).use {
            val oos = ObjectOutputStream(it)
            oos.writeObject(LocalDateTime.now())
            oos.writeObject(game)
            oos.close()
            it.close()
        }
    }

    private fun loadGamePressed() {
        try {
            baseContext.openFileInput(SAVED_GAME).use {

                val ois = ObjectInputStream(it)
                val ldt = ois.readObject() as LocalDateTime
                val g = ois.readObject() as Game
                ois.close()
                it.close()
                AlertDialog.Builder(this).apply {
                    setPositiveButton(getString(R.string.ok),
                        DialogInterface.OnClickListener { dialog, which -> game = g; resumePressed() })
                    setNegativeButton(getString(R.string.cancel), null)
                    setTitle(R.string.continue_this_game)
                    setMessage(getString( R.string.saved_at) + ldt.format(DateTimeFormatter.ofPattern("dd/MM/YYYY - HH:mm:ss")))
                }.show()


            }
        } catch ( e:  FileNotFoundException) {
            Toast.makeText(this, getString(R.string.no_saved_game), Toast.LENGTH_LONG).show()
        }

    }



    private fun setColorSliders(color: String) {
        binding.greenSB.progress = Color.parseColor(color).green
        binding.blueSB.progress = Color.parseColor(color).blue
        binding.redSB.progress = Color.parseColor(color).red
    }


    override fun onSaveInstanceState(outState: Bundle) {
        // Saves the status of the activity
        outState?.run {
            putParcelable(GAME, game)
            putBoolean(IS_TIME_ACTIVE, binding.playWithTimeSW.isChecked)
            putBoolean(CUSTOM_BOARD_ACTIVE, binding.customBoardSW.isChecked)
            putInt(RED_SLIDER, binding.redSB.progress)
            putInt(BLUE_SLIDER, binding.blueSB.progress)
            putInt(GREEN_SLIDER, binding.greenSB.progress)
            putInt(MINUTES, Integer.parseInt ( binding.minutesET.text.toString() ))
            putInt(PLAYER2_LEVEL, binding.player2Spinner.selectedItemPosition)

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

        }

    }







    /*
    private val spinnerListener = object: AdapterView.OnItemSelectedListener {
        override fun onItemSelected (parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
            //do something when is selected.
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            TODO("Not yet implemented")
        }
    }*/

    /**A SeekBarListener for color selection*/
    private val seekBarListener = object: SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val newColor = "#" + String.format("%02X", binding.redSB.progress) + String.format("%02X", binding.greenSB.progress) + String.format("%02X", binding.blueSB.progress)
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