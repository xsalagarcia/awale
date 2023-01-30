package sala.xevi.awale.models

import sala.xevi.awale.exceptions.IllegalMovementException
import java.util.*

class AwePlayer {

    companion object {

        /**The last move that AwePlayer did*/
        var lastMov: Int? = null

        /**
         * Given a game, returns a move with a deep search, with specified maximum deep.
         * @param game The game representation.
         * @param depth The maximum level of the search.
         * @return An Int that represents the box to move.
         */
        fun play(game: Game, depth: Int): Int {

            val firstBoxToCheck = if (game.activePlayer == game.player1) 7 else 0
            val listOfMoves = mutableMapOf<Int, Game>() //a list of moves
            for (i in firstBoxToCheck..firstBoxToCheck + 5) {
                try {
                    val resultGame = game.copyMe()
                    resultGame.playBox(i)
                    listOfMoves[i] = resultGame

                } catch (e: IllegalMovementException) {
                    //do nothing
                }
            }

            val scoreOfMovements = mutableMapOf<Int, Int>() //key = movement, key = score got by alphaBetaPrunning
            listOfMoves.forEach { (mov, game) ->
                scoreOfMovements[mov] = alphaBetaPruning(game, depth, Int.MIN_VALUE, Int.MAX_VALUE)
            }

            lastMov = bestMove(scoreOfMovements, game.isPlayer1Active())
            return lastMov!!

        }



        private fun alphaBetaPruning (node: Game, depth: Int, alpha: Int, beta: Int): Int{

            var newBeta = beta
            var newAlpha = alpha
            if (depth == 0 ) {
                return node.scoreDifferenceP1minusP2()
            }

            val listOfMovements =  mutableMapOf<Int, Game>()
            val firstBoxToCheck = if (node.activePlayer == node.player1) 6 else 0
            for (i in firstBoxToCheck..firstBoxToCheck+5){
                try {
                    val childNode = node.copyMe()
                    childNode.playBox(i)
                    listOfMovements[i] = childNode

                } catch (e: IllegalMovementException){
                    //do nothing
                }
            }

            if (listOfMovements.isEmpty()){ // No movements possible (finished game). Terminal node.
                node.isGameFinished() //updates the players scores if there is no possible movement.
                if (node.scoreDifferenceP1minusP2() > 0) return Int.MAX_VALUE //player1 wins
                else if (node.scoreDifferenceP1minusP2() < 0) return Int.MIN_VALUE //player2 wins
                return node.scoreDifferenceP1minusP2() //It's a tie
            }

            if (!node.isPlayer1Active()){ //player2 is min
                val iterator = listOfMovements.iterator()
                while (iterator.hasNext() && newAlpha < newBeta) {
                    val value = alphaBetaPruning (iterator.next().value, depth-1, newAlpha, newBeta)
                    if (value< newBeta) {
                        newBeta = value
                    }
                }
                return newBeta
            }

            else { //activePlayer is player1, max player
                val iterator = listOfMovements.iterator()
                while (iterator.hasNext() && newAlpha < newBeta) {
                    val value = alphaBetaPruning (iterator.next().value, depth-1, newAlpha, newBeta)
                    if (value > newAlpha) {
                        newAlpha = value
                    }
                }
                return newAlpha
            }
        }

        /**
         * Given a map of moves <movement, score> returns the best option. The move is represented by the number of the box where the move starts.
         * @param movementsMap is a Map<Int, Int> where the key is the number corresponding the box to move. The value is an integer that represents player1.score - player2.score after the move.
         * @param player1Active is a boolean value that will be true if player1 is active. That is, best move is the one that obtains the lower difference between player1.score - player2.score after the move.
         */
        private fun bestMove(movementsMap: Map<Int, Int>, player1Active: Boolean): Int {

            val filteredMap = if (player1Active) {
                movementsMap.filterValues { value -> movementsMap.values.max() == value }
            } else {
                movementsMap.filterValues { value -> movementsMap.values.min() == value }
            }
            return filteredMap.keys.elementAt(Random().nextInt(filteredMap.size))

        }
    }
}