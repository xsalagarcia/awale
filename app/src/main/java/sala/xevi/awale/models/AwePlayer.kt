package sala.xevi.awale.models

import sala.xevi.awale.exceptions.IllegalMovementException
import java.util.*

/**
 * Contains methods for AI move.
 */
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


            val listOfMoves = getPossibleMoves(game)

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

            val listOfMovements = getPossibleMoves(node)

            if (listOfMovements.isEmpty()){ // No movements possible (finished game). Terminal node.
                return getFinishedGameValue(node)
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
         * Given a map of moves <movement, score> returns the best option.
         * The move is represented by the number of the box where the move starts.
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

        /**
         * Returns a map where key is the number of the box move and value is the resulting game.
         */
        private fun getPossibleMoves(game: Game): MutableMap<Int, Game> {
            val listOfMoves =  mutableMapOf<Int, Game>()
            val firstBoxToCheck = if (game.activePlayer == game.player1) 6 else 0
            for (i in firstBoxToCheck..firstBoxToCheck+5){
                try {
                    val childGame = game.copyMe()
                    childGame.playBox(i)
                    listOfMoves[i] = childGame

                } catch (e: IllegalMovementException){
                    //do nothing
                }
            }
            return listOfMoves
        }

        /**
         * Because the value of finished game isn't the difference between player1 and player2
         */
        private fun getFinishedGameValue(node: Game): Int {
            node.isGameFinished() //updates the players scores if there is no possible movement.
            if (node.scoreDifferenceP1minusP2() > 0) return Int.MAX_VALUE //player1 wins
            else if (node.scoreDifferenceP1minusP2() < 0) return Int.MIN_VALUE //player2 wins
            return node.scoreDifferenceP1minusP2() //It's a tie
        }
    }
}