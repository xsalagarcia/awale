package sala.xevi.awale.models

import android.os.Parcel
import android.os.Parcelable
import sala.xevi.awale.exceptions.IllegalMovementException
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

//https://es.wikipedia.org/wiki/Oware
//https://www.myriad-online.com/resources/docs/awale/espanol/rules.htm
//https://ca.wikipedia.org/wiki/Aual%C3%A9

/**
 * Class representing the game.
 */
class Game (/**Player1, always human*/): Parcelable, Externalizable {

    lateinit var player1: Player

    lateinit var player2: Player

    lateinit var activePlayer: Player  //The player which has to move.

    /**representation of boxes.*/
    var boxes: IntArray = intArrayOf(4,4,4,4,4,4,4,4,4,4,4,4)

    /**representation of last state of the boxes.*/
    var lastStateBoxs: IntArray = intArrayOf(4,4,4,4,4,4,4,4,4,4,4,4)

    /**Number of seeds reaps at the last move*/
    var reapsLastMov: Int = 0

    //parcelable
    constructor(parcel: Parcel) : this() {
        player1 = parcel.readParcelable(Player::class.java.classLoader)!!
        player2 = parcel.readParcelable(Player::class.java.classLoader)!!
        activePlayer = if (parcel.readInt() == 1) player1 else player2
        boxes = parcel.createIntArray()!!
        lastStateBoxs = parcel.createIntArray()!!
        reapsLastMov = parcel.readInt()
    }

    constructor(player1: Player, player2: Player) : this() {
        this.player1 = player1
        this.player2 = player2
        activePlayer = player1
    }


    fun changeActivePlayer(){
        activePlayer = if (activePlayer == player1) player2 else player1
    }

    fun isPlayer1Active() : Boolean{
        return activePlayer == player1
    }

    /**
     * Plays a movement at [boxToPlay]. If the movement isn't legal throws IllegalmovementException
     * @param boxToPlay The box to play.
     * @throws IllegalMovementException if the movement isn't legal.
     */
    @Throws(IllegalMovementException::class)
    fun playBox(boxToPlay: Int) {

        if (boxes[boxToPlay] == 0) throw IllegalMovementException()

        lastStateBoxs = boxes.copyOf()
        player1.previousScore = player1.score
        player2.previousScore = player2.score

        var seeds = boxes[boxToPlay]

        //sowing
        boxes[boxToPlay] = 0
        var seedsToSow = seeds
        var sowingBox = boxToPlay+1
        while(seedsToSow>0){
            if (!(seeds > 11 && boxToPlay == (sowingBox)%12)) { //https://www.myriad-online.com/resources/docs/awale/espanol/rules.htm rule 6
                boxes[(sowingBox) % 12] = boxes[(sowingBox) % 12] + 1
                seedsToSow--
            }
            sowingBox++
        }
        /*for (i in 1 .. seeds){
            if (!(seeds > 11 && boxToPlay == (boxToPlay + i)%12)) { //https://www.myriad-online.com/resources/docs/awale/espanol/rules.htm rule 6
                boxes[(boxToPlay + i) % 12] = boxes[(boxToPlay + i) % 12] + 1
            }
        }*/

        //reap
        //(sowingBox-1)%12 és la última en ser plantada
        reapsLastMov = 0
        var boxToReap = 12+(sowingBox-1)%12
        seeds += 12
        var capture = true // rule 5
        while (capture) {
            if (boxes[boxToReap%12] == 2 || boxes[boxToReap%12] == 3){
                if  ( ( (boxToReap%12 < 6) && activePlayer == player1) || (boxToReap%12 > 5 && activePlayer == player2) ) {
                    activePlayer.score = boxes[boxToReap%12] + activePlayer.score
                    boxes[boxToReap%12] = 0
                    boxToReap--
                    reapsLastMov++
                } else {
                    capture = false
                }
            } else {
                capture = false
            }
        }
        /*
        //reap
        //(sowingBox-1)%12 és la última en ser plantada
        reapsLastMov = 0
        if (seeds > 11) seeds++
        seeds += 12
        var capture = true // rule 5
        while (capture) {
            if (boxes[(seeds+boxToPlay) % 12] == 2 || boxes[(seeds+boxToPlay) % 12] == 3){
                if  ( ( ((seeds+boxToPlay)%12 < 6) && activePlayer == player1) || ((seeds+boxToPlay)%12 > 5 && activePlayer == player2) ) {
                    activePlayer.score = boxes[(seeds + boxToPlay) % 12] + activePlayer.score
                    boxes[(seeds + boxToPlay) % 12] = 0
                    seeds--
                    reapsLastMov++
                } else {
                    capture = false
                }
            } else {
                capture = false
            }
        }*/

        if (totalSeedsAtPlayersField(getInactivePlayer())==0) {
            boxes = lastStateBoxs.copyOf()
            activePlayer.score = activePlayer.previousScore
            throw IllegalMovementException()
        }

        changeActivePlayer()
    }

    /**
     * Returns the number of seeds at the side of the [player]
     * @param player The side of the player to be evaluated. Must be game.player1 or game.player2.
     * @return An Int representing the quantity of the seeds.
     */
    private fun totalSeedsAtPlayersField(player: Player): Int {
        assert(player == player1 || player == player2)
        var totalSeeds: Int = 0
        val startingBox = if (player == player1) 6 else 0
        for (i in startingBox..(startingBox+5)){
            totalSeeds += boxes[i]
        }

        return totalSeeds
    }

    /**
     * Returns the inactive player.
     * @return The inactive [Player]
     */
    private fun getInactivePlayer(): Player {
        return if (activePlayer == player1) player2
        else player1
    }

    /**
     * Checks if game is finished. If it's, returns true.
     * @return true if game is finished
     */
    fun isGameFinished(): Boolean {
        var isFinished = true

        if (player1.score > 24 || player2.score > 24) return isFinished

        var firstBoxToCheck: Int
        if (activePlayer == player1) {
            firstBoxToCheck = 6
        } else {
            firstBoxToCheck = 0
        }

        val gameForCheck = copyMe()
        for (i in firstBoxToCheck..(firstBoxToCheck+5)){ //Test if there is no possible movement
            try {
                gameForCheck.playBox(i)
                isFinished = false
                break
            } catch (e: IllegalMovementException) {
                //do nothing
            }
        }
        return isFinished
    }

    /**
     * Returns a copy of the present game.
     * @return An object representing a copy of the present game.
     */
    fun copyMe (): Game {
        val gameCopied= Game(Player(player1.name, player1.score), Player(player2.name, player2.score))
        if (gameCopied.isPlayer1Active() != isPlayer1Active()) gameCopied.changeActivePlayer()
        gameCopied.boxes = boxes.copyOf()
        gameCopied.lastStateBoxs = lastStateBoxs.copyOf()
        return gameCopied
    }

    /**
     * Undo last movement if it's possible.
     */
    fun undoLastMov(){

        var cantUndo = true
        for (i in 0..boxes.size-1){
            if (boxes[i] != lastStateBoxs[i]) {
                cantUndo = false
                break
            }
        }
        if (cantUndo) return

        player1.score = player1.previousScore
        player2.score = player2.previousScore
        boxes = lastStateBoxs.copyOf()
        changeActivePlayer()

    }

    fun scoreDifferenceP1minusP2():Int{
        return player1.score - player2.score
    }

    //Parcelable
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(player1, flags)
        parcel.writeParcelable(player2, flags)
        parcel.writeInt(if (isPlayer1Active())1 else 0)
        parcel.writeIntArray(boxes)
        parcel.writeIntArray(lastStateBoxs)
        parcel.writeInt(reapsLastMov)
    }

    //Parcelable
    override fun describeContents(): Int {
        return 0
    }

    //parcelable
    companion object CREATOR : Parcelable.Creator<Game> {
        override fun createFromParcel(parcel: Parcel): Game {
            return Game(parcel)
        }

        override fun newArray(size: Int): Array<Game?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeExternal(out: ObjectOutput?) {
        out!!.apply{
            writeObject(player1)
            writeObject(player2)
            writeBoolean(isPlayer1Active())
            writeObject(boxes)
            writeObject(lastStateBoxs)
            writeInt(reapsLastMov)
        }
    }

    override fun readExternal(oi: ObjectInput?) {
        oi!!.apply {
            player1 = readObject() as Player
            player2 = readObject() as Player
            activePlayer = if(readBoolean()) player1 else player2
            boxes = readObject() as IntArray
            lastStateBoxs = readObject() as IntArray
            reapsLastMov = readInt()

        }
    }


}