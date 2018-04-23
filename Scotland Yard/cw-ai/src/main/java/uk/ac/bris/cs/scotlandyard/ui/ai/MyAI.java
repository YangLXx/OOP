package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.*;


@ManagedAI("mrX")
public class MyAI implements PlayerFactory {

	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player{
		// Modifiable scalars
		final int doubleBetterThanTicket = 1;
		final int useDoubleMove = 3;
		final int secretMoveScalar = 1;

		// Override methods using
		int moveDestination;
		Ticket ticketMoveTicket;
		Ticket firstMoveTicket;
		Ticket secondMoveTicket;
		int firstMoveDestination;
		Map<Move, Integer> ticketMoveScores = new HashMap<>();
		Map<Move, Integer> doubleMoveScores = new HashMap<>();

		// Copy the number of remained tickets from ScotlandYardView to a new Map<Ticket, Integer>
		// for counting the remaining tickets in searching of least steps
		private void setTargetDetectiveTickets (ScotlandYardView view,
								 Colour targetDetective,
								 Map<Ticket, Integer> targetDetectiveTickets){
			if (view.getPlayerTickets(targetDetective, Ticket.TAXI).isPresent()
					&& view.getPlayerTickets(targetDetective, Ticket.BUS).isPresent()
					&& view.getPlayerTickets(targetDetective, Ticket.UNDERGROUND).isPresent()) {
				targetDetectiveTickets.put(Ticket.TAXI, view.getPlayerTickets(targetDetective, Ticket.TAXI).get());
				targetDetectiveTickets.put(Ticket.BUS, view.getPlayerTickets(targetDetective, Ticket.BUS).get());
				targetDetectiveTickets.put(Ticket.UNDERGROUND, view.getPlayerTickets(targetDetective, Ticket.UNDERGROUND).get());
			}
		}

		// Start search from current node
		private void startSearchingFrom (Node<Integer> currentNode,
										Graph<Integer, Transport> graph,
										Map<Ticket, Integer> targetDetectiveTickets,
										HashMap<Node<Integer>, Integer> steps){
			// Get the possible edges from currentNode
			Collection<Edge<Integer, Transport>> possibleEdges = new HashSet<>(graph.getEdgesFrom(currentNode));
		    
			for (Edge<Integer, Transport> possibleEdge : possibleEdges) {
				if (targetDetectiveTickets.get(Ticket.fromTransport(possibleEdge.data())) != 0 &&
						(steps.get(currentNode) + 1 < steps.get(possibleEdge.destination()) ||
						steps.get(possibleEdge.destination()) == 0)){
					// Set the value to n + 1
					steps.replace(possibleEdge.destination(), steps.get(currentNode) + 1);
					// Decrease the consumed ticket
					targetDetectiveTickets.replace(Ticket.fromTransport(possibleEdge.data()),
							targetDetectiveTickets.get(Ticket.fromTransport(possibleEdge.data())) - 1);
					// Start searching from next node
					startSearchingFrom(possibleEdge.destination(), graph, targetDetectiveTickets, steps);
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
				if (view.getPlayerLocation(targetDetective).isPresent()
						&& node.equals(new Node<>(view.getPlayerLocation(targetDetective).get())))
					startSearchingFrom(node, view.getGraph(), targetDetectiveTickets, steps);
			}

			return steps.get(new Node<>(destination));
		}

		private int totalLeastSteps (ScotlandYardView view, int moveDestination){
			int totalLeastSteps = 0;
			for (Colour player : view.getPlayers()){
				if (Dijkstras(view, moveDestination, player) == 1) return 0;
				else totalLeastSteps = totalLeastSteps + Dijkstras(view, moveDestination, player);
			}
			return totalLeastSteps;
		}

		private int score (ScotlandYardView view, Move move){
			int secretMoveWeight = -secretMoveScalar;
			int furtherEdges = 0;
			// Create MoveVisitors and override visit to get final destination for different moves
			MoveVisitor ticketMoveVisitor = new MoveVisitor() {
				@Override
				public void visit(TicketMove move) {
					moveDestination = move.destination();
					ticketMoveTicket = move.ticket();
				}
			};
			MoveVisitor doubleMoveVisitor = new MoveVisitor() {
				@Override
				public void visit(DoubleMove move) {
					firstMoveDestination = move.firstMove().destination();
					moveDestination = move.finalDestination();
					firstMoveTicket = move.firstMove().ticket();
					secondMoveTicket = move.secondMove().ticket();
				}
			};
			// If the move round is revealed, the score of move should consider how many edges from move destination
			// in order to confuse detectives
			if (move.getClass().equals(TicketMove.class)) {
				move.visit(ticketMoveVisitor);
				if (view.getRounds().get(view.getCurrentRound() + 1)) furtherEdges = view.getGraph().getEdgesFrom(new Node<> (moveDestination)).size();
			}
			if (move.getClass().equals(DoubleMove.class)) {
				move.visit(doubleMoveVisitor);
				if (view.getRounds().get(view.getCurrentRound() + 1)) furtherEdges = view.getGraph().getEdgesFrom(new Node<> (firstMoveDestination)).size();
				if (view.getRounds().get(view.getCurrentRound() + 2)) furtherEdges = view.getGraph().getEdgesFrom(new Node<> (moveDestination)).size();
			}
			// MrX will choose SECRET move if and only if when mrX is going to use UNDERGROUND ticket
			for (Edge<Integer, Transport> edge : view.getGraph().getEdges()){
				if (edge.data().equals(Transport.UNDERGROUND)) {
					if (move.getClass().equals(TicketMove.class)
							&& ticketMoveTicket.equals(Ticket.SECRET)
							&& view.getPlayerLocation(Colour.BLACK).isPresent()
							&& view.getPlayerLocation(Colour.BLACK).get().equals(edge.source().value())
							&& moveDestination == edge.destination().value())
						secretMoveWeight = secretMoveScalar;
					if (move.getClass().equals(DoubleMove.class) &&
							((firstMoveTicket.equals(Ticket.SECRET)
									&& view.getPlayerLocation(Colour.BLACK).isPresent()
									&& view.getPlayerLocation(Colour.BLACK).get().equals(edge.source().value())
									&& firstMoveDestination == edge.destination().value())
									|| (secondMoveTicket.equals(Ticket.SECRET)
									&& firstMoveDestination == edge.source().value()
									&& moveDestination == edge.destination().value())
							))
						secretMoveWeight = secretMoveScalar;
				}
			}
			return totalLeastSteps(view, moveDestination) + furtherEdges + secretMoveWeight;
		}

		boolean moreThanOneDetectiveTwoStepsToMrX (ScotlandYardView view){
			int counter = 0;
			for (Colour player : view.getPlayers()){
				if (player.isDetective()
						&& view.getPlayerLocation(Colour.BLACK).isPresent()
						&& Dijkstras(view, view.getPlayerLocation(Colour.BLACK).get(), view.getCurrentPlayer()) <= 2)
					counter ++;
			}
			return counter > 1;
		}

		boolean detectiveWithinOneSteps (ScotlandYardView view){
			int counter = 0;
			for (Colour player : view.getPlayers()){
				if (player.isDetective()
						&& view.getPlayerLocation(Colour.BLACK).isPresent()
						&& Dijkstras(view, view.getPlayerLocation(Colour.BLACK).get(), view.getCurrentPlayer()) <= 1)
					counter ++;
			}
			return counter > 0;
		}

		private int getHighestScore (Map<Move, Integer> moveScores){
			int highestScore = 0;
			int scoreBefore = 0;
			for (Integer score : moveScores.values()){
				if (score > scoreBefore) highestScore = score;
				scoreBefore = score;
			}
			return highestScore;
		}

		private ArrayList<Move> pickBestMoves (ScotlandYardView view,
											   Map<Move, Integer> ticketMoveScores,
											   Map<Move, Integer> doubleMoveScores){
			ArrayList<Move> bestMoves = new ArrayList<>();
			final int doubleMoveLine = (view.getPlayers().size() - 1) * useDoubleMove;
			int highestScoreInTicketMove = getHighestScore(ticketMoveScores);
			int highestScoreInDoubleMove = getHighestScore(doubleMoveScores);
			if ((highestScoreInTicketMove < doubleMoveLine
					|| moreThanOneDetectiveTwoStepsToMrX(view)
					|| detectiveWithinOneSteps(view)
					|| highestScoreInDoubleMove - highestScoreInTicketMove > view.getPlayers().size() * doubleBetterThanTicket)
					&& view.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).isPresent()
					&& view.getPlayerTickets(Colour.BLACK, Ticket.DOUBLE).get() != 0){
				for (Move move : doubleMoveScores.keySet())
					if (ticketMoveScores.get(move).equals(highestScoreInDoubleMove))
						bestMoves.add(move);
				return bestMoves;
			}
			else {
				for (Move move : ticketMoveScores.keySet())
					if (ticketMoveScores.get(move).equals(highestScoreInTicketMove))
						bestMoves.add(move);
				return bestMoves;
			}
		}

		@Override
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
							 Consumer<Move> callback) {
			ArrayList<Move> bestMoves;
			Move theBestMove;
			MoveVisitor ticketMoveVisitor = new MoveVisitor() {
				@Override
				public void visit(TicketMove move) {
					ticketMoveScores.put(move, score(view, move));
				}
			};
			MoveVisitor doubleMoveVisitor = new MoveVisitor() {
				@Override
				public void visit(DoubleMove move) {
					doubleMoveScores.put(move, score(view, move));
				}
			};
			// Split valid moves into two sets: single move and double move
			for (Move move : moves) {
				if (move.getClass().equals(TicketMove.class)) move.visit(ticketMoveVisitor);
				if (move.getClass().equals(DoubleMove.class)) move.visit(doubleMoveVisitor);
			}

			// Pick the best moves in a set by comparing the score
			bestMoves = pickBestMoves(view, ticketMoveScores, doubleMoveScores);
			// Pick one randomly as theBestMove
			final Random random = new Random();
			theBestMove = bestMoves.get(random.nextInt(bestMoves.size()));
			// callback
			callback.accept(theBestMove);
		}
	}
}
