package sala.xevi.awale.models

import sala.xevi.awale.exceptions.IllegalMovementException

//https://es.wikipedia.org/wiki/Oware
//https://www.myriad-online.com/resources/docs/awale/espanol/rules.htm
//https://ca.wikipedia.org/wiki/Aual%C3%A9

/**
 * Class representing the game.
 */
class Game (var player1: Player, var player2: Player) {
    var activePlayer: Player = player1 //The player which has to move.
    var boxes: IntArray = intArrayOf(4,4,4,4,4,4,4,4,4,4,4,4) //representation of boxes.
    var lastStateBoxs: IntArray = intArrayOf(4,4,4,4,4,4,4,4,4,4,4,4) //representation of last state of the boxes.
    var reapsLastMov: Int = 0 //number of seeds reaps at the last movement


    fun changeActivePlayer(){
        if (activePlayer == player1) {
            activePlayer = player2
        }else {
            activePlayer = player1
        }
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

        lastStateBoxs = boxes.copyOf()
        player1.previousScore = player1.score
        player2.previousScore = player2.score

        var seeds = boxes[boxToPlay]

        //sowing
        boxes[boxToPlay] = 0
        for (i in 1 .. seeds){
            if (!(seeds > 11 && boxToPlay == (boxToPlay + i)%12)) { //https://www.myriad-online.com/resources/docs/awale/espanol/rules.htm rule 6
                boxes[(boxToPlay + i) % 12] = boxes[(boxToPlay + i) % 12] + 1
            }
        }

        //reap
        reapsLastMov = 0
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
        }

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
        if (player == player1) {
            for (i in 0..5){
                totalSeeds += boxes[i]
            }
        } else {
            for (i in 6..11){
                totalSeeds += boxes[i]
            }
        }
        return totalSeeds
    }

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
            firstBoxToCheck = 0
        } else {
            firstBoxToCheck = 6
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
        val gameCopied= Game(Player(player1.name, player1.levelAI,player1.score), Player(player2.name, player2.levelAI, player2.score))
        if (gameCopied.isPlayer1Active() != isPlayer1Active()) gameCopied.changeActivePlayer()
        gameCopied.boxes = boxes.copyOf()
        gameCopied.lastStateBoxs = lastStateBoxs.copyOf()
        return gameCopied

    }





}