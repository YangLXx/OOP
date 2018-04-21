package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
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

		int moveDestination;

		// Copy the number of remained tickets from ScotlandYardView to a new Map<Ticket, Integer>
		// for counting the remaining tickets in searching of least steps
		private void setTargetDetectiveTickets (ScotlandYardView view,
								 Colour targetDetective,
								 Map<Ticket, Integer> targetDetectiveTickets){
			targetDetectiveTickets.put(Ticket.TAXI, view.getPlayerTickets(targetDetective, Ticket.TAXI).get());
			targetDetectiveTickets.put(Ticket.BUS, view.getPlayerTickets(targetDetective, Ticket.BUS).get());
			targetDetectiveTickets.put(Ticket.UNDERGROUND, view.getPlayerTickets(targetDetective, Ticket.UNDERGROUND).get());

		}

		// Start search from current node
		public void startSearchingFrom (Node<Integer> currentNode,
										Graph<Integer, Transport> graph,
										Map<Ticket, Integer> targetDetectiveTickets,
										HashMap<Node<Integer>, Integer> steps){
			// Get the possible edges from currentNode
			Collection<Edge<Integer, Transport>> possibleEdges = new HashSet<>(graph.getEdgesFrom(currentNode));

			for (Edge<Integer, Transport> possibleEdge : possibleEdges) {
				if (targetDetectiveTickets.get(Ticket.fromTransport(possibleEdge.data())) != 0 &&
						(currentNode.value() + 1 < steps.get(possibleEdge.destination()) ||
						steps.get(possibleEdge.destination()) == 0)){
					// Create a new Map for tickets
					Map<Ticket, Integer> targetDetectiveCURRENTTickets = targetDetectiveTickets;
					// Set the value to n + 1
					steps.replace(possibleEdge.destination(), currentNode.value() + 1);
					// Decrease the consumed ticket
					targetDetectiveCURRENTTickets.replace(Ticket.fromTransport(possibleEdge.data()),
							targetDetectiveTickets.get(Ticket.fromTransport(possibleEdge.data())) - 1);
					// Start searching from next node
					startSearchingFrom(possibleEdge.destination(), graph, targetDetectiveCURRENTTickets, steps);
				}
			}
		}

		// Return the lease steps form the position of targetDetective to the destination
		private int Dijkstras (ScotlandYardView view, int destination, Colour targetDetective){
			// Create a new Map of Tickets to recode the remaining tickets of the targetDetective
			Map<Ticket, Integer> targetDetectiveTickets = new HashMap<>();
			// Create a Map for recording steps from targetDetective to each node
			HashMap<Node<Integer>, Integer> steps = new HashMap<>();
			setTargetDetectiveTickets(view, targetDetective, targetDetectiveTickets);
			// Set steps all zero initially
			for (Node<Integer> node : view.getGraph().getNodes()) steps.put(node, 0);

			// Locate the location of targetDetective and start searching from there
			for (Node<Integer> node: steps.keySet()) {
				if (node.equals(new Node<>(view.getPlayerLocation(targetDetective).get()))){
					startSearchingFrom(node, view.getGraph(), targetDetectiveTickets, steps);
				}
			}

			return steps.get(new Node<>(destination));
		}

		private int totalLeastSteps (ScotlandYardView view, int moveDestination){
			int totalLeastSteps = 0;
			for (Colour player : view.getPlayers()){
				totalLeastSteps = totalLeastSteps + Dijkstras(view, moveDestination, player);
			}
			return totalLeastSteps;
		}

		private int score (ScotlandYardView view, Move move){
			// Create MoveVisitors and override visit to get final destination for different moves
			MoveVisitor ticketMoveVisitor = new MoveVisitor() {
				@Override
				public void visit(TicketMove move) {
					moveDestination = move.destination();
				}
			};
			MoveVisitor doubleMoveVisitor = new MoveVisitor() {
				@Override
				public void visit(DoubleMove move) {
					moveDestination = move.finalDestination();
				}
			};
			if (move.getClass().equals(TicketMove.class)) move.visit(ticketMoveVisitor);
			if (move.getClass().equals(DoubleMove.class)) move.visit(doubleMoveVisitor);

			int totalLeastSteps = totalLeastSteps(view, moveDestination);
			// TODO furtherValidmoves is much better than furtherEdges
			int furtherEdges = view.getGraph().getEdgesFrom(new Node<Integer> (moveDestination)).size();
			int FengShui;
			boolean thisRound = view.getRounds().get(view.getCurrentRound());
			boolean nextRound = view.getRounds().get(view.getCurrentRound() + 1);
			//Give the formula of score
			//int score =...
			return 0;
		}

		private ArrayList<Move> pickBestMoves (Map<Move, Integer> moveScores){
			ArrayList<Move> bestMoves = new ArrayList<>();
			// TODO
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
			// TODO
			// callback
			callback.accept(theBestMove);
		}
	}
}
