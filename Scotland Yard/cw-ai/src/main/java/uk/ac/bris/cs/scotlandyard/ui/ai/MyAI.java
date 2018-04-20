package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;


@ManagedAI("Easy")
public class MyAI implements PlayerFactory {

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player{
		private int Dijkstras (ScotlandYardView view, int destination, Colour targetDetective){
			Graph<Integer, Transport> graph = view.getGraph();
			HashMap<Node<Integer>, Integer> steps = new HashMap<>();
			
			return 0;
		}




		private Map<Move, Integer> leastSteps (ScotlandYardView view, Move move) {
 			Map<Move, Integer> leastSteps = new HashMap<>();
 			return leastSteps;
		}

		private int score (ScotlandYardView view, Move move){
			int furtherEdgesScore;
			boolean thisRound = view.getRounds().get(view.getCurrentRound());
			boolean nextRound = view.getRounds().get(view.getCurrentRound() + 1);
			//Give the formula of score
			//int score =...
			return 0;
		}

		private ArrayList<Move> pickBestMoves (Map<Move, Integer> moveScores){
			ArrayList<Move> bestMoves = new ArrayList<>();
			return bestMoves;
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
