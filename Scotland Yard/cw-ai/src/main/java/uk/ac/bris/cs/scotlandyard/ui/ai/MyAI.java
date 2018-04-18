package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;

// TODO name the AI
@ManagedAI("For Losers")
public class MyAI implements PlayerFactory {

	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	// TODO A sample player that selects a random move
	/*
	private static class MyPlayer implements Player {

		private final Random random = new Random();

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			// TODO do something interesting here; find the best move
			// picks a random move
			callback.accept(new ArrayList<>(moves).get(random.nextInt(moves.size())));

		}
	}
	*/

	private static class MyPlayer implements Player{

		private int Dijkstras (ScotlandYardView view, Move move, Colour targetDetective){

		}

		private int leastSteps (ScotlandYardView view, Move move) {

		}

		private int score (ScotlandYardView view, Move move){
			int leastSteps = leastSteps(view, move);
			int edgesOfDestination;
			boolean thisRound = view.getRounds().get(view.getCurrentRound());
			boolean nextRound = view.getRounds().get(view.getCurrentRound() + 1);
			//Give the formula of score
			//int score =...
			return score;
		}

		private ArrayList<Move> pickBestMoves (Map<Move, Integer> moveScores){

		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
							 Consumer<Move> callback) {
			Map<Move, Integer> moveScores = new HashMap<>();
			ArrayList<Move> bestMoves;
			Move theBestMove;

			// Get all the score of move and put in a Map
			for (Move move : moves) moveScores.put(move, score(view, move));
			// Pick the best moves in a set by comparing the score
			bestMoves = pickBestMoves(moveScores);
			// Plan A: Just pick one randomly as theBestMove
			final Random random = new Random();
			theBestMove = bestMoves.get(random.nextInt(bestMoves.size()));
			// Plan B: Considering one more step or using another standard to choose theBestMove
			// callback
			callback.accept(theBestMove);
		}
	}
}
