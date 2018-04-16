package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import javax.security.auth.login.Configuration;

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor {

	List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
	private ArrayList<ScotlandYardPlayer> players = new ArrayList<>();
	private Integer playerMoveCount = 0;
	private Integer roundCounter = NOT_STARTED;
	private List<Spectator> spectators = new CopyOnWriteArrayList<>();
	private Move currentMove;
	private int mrXLocation = 0;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		//Check round and graph is NULL and Empty or not.
		this.rounds = requireNonNull(rounds);
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}
		this.graph = requireNonNull(graph);
		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}

		//Check mrX is black or not.
		if (mrX.colour != BLACK) {
			throw new IllegalArgumentException("MrX should be Black");
		}

		//Check mrX and Detectives are Null, same location or color or not
		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(requireNonNull(configuration));
		configurations.add(0,firstDetective);
		configurations.add(0,mrX);
		Set<Integer> set = new HashSet<>();
		Set<Colour> colorset = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set.add(configuration.location);
			if (colorset.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate Colour");
			colorset.add(configuration.colour);

		}

		// Check tickets correctly exist
		for (PlayerConfiguration configuration : configurations) {
			for(Ticket ticket : Ticket.values())
				if (!configuration.tickets.containsKey(ticket))
					throw new IllegalArgumentException("Detectives miss ticket");
		}
		if (firstDetective.tickets.get(DOUBLE) > 0 || firstDetective.tickets.get(SECRET) > 0)
			throw new IllegalArgumentException("Detective has illegal ticket");
		for (PlayerConfiguration configuration : restOfTheDetectives) {
			if (configuration.tickets.get(DOUBLE) > 0 || configuration.tickets.get(SECRET) > 0)
				throw new IllegalArgumentException("Detectives has illegal ticket");
		}

		// Copy data from PlayerConfiguration(default) to ScotlandYardPlayer
		for (PlayerConfiguration player : configurations){
		    ScotlandYardPlayer p = new ScotlandYardPlayer(
		            player.player,
                    player.colour,
		            player.location,
                    player.tickets
            );
		    players.add(p);
        }
	}

	@Override
    public void accept(Move move){
	    // Check the para move is null or not valid
	    move = requireNonNull(move);
	    if (!validMove(move.colour()).contains(move)) throw new IllegalArgumentException("Invalid move");
	    else {
	        //Override visit method for TicketMove from MoveVisitor interface
            MoveVisitor ticketMoveVisitor = new MoveVisitor() {
                @Override
                public void visit(TicketMove move) {
                	currentMove = move;
                    for (ScotlandYardPlayer player : players) {
                        if (player.colour() == move.colour()) {
                            player.tickets().replace(move.ticket(), player.tickets().get(move.ticket()) - 1);
                            player.location(move.destination());
							if (move.colour().isMrX()) {
								roundCounter++;
							}
                            if(player.isDetective()) {
                                players.get(0).tickets().replace(move.ticket(), players.get(0).tickets().get(move.ticket()) + 1);
                            }
                            // Round = 1, CurrentPlayer = BLUE
							playerMoveCount ++;
							if (move.colour().isMrX()) onRoundStarted();
                            onMoveMade();
                        }
                    }
                }
            };
            //Override visit method for DoubleMove from MoveVisitor interface
            MoveVisitor doubleMoveVisitor = new MoveVisitor() {
                @Override
                public void visit(DoubleMove move) {
                	Integer firstDestination;
                	Integer secondDestination;
                	for (ScotlandYardPlayer player : players) {
                		if (player.colour() == move.colour()) {
							player.tickets().replace(DOUBLE, player.tickets().get(DOUBLE) - 1);
							if (getRounds().get(getCurrentRound())) firstDestination = move.firstMove().destination();
							else firstDestination = mrXLocation;
							if (getRounds().get(getCurrentRound() + 1)) secondDestination = move.secondMove().destination();
							else secondDestination = mrXLocation;
							currentMove = new DoubleMove(player.colour(), move.firstMove().ticket(), firstDestination, move.secondMove().ticket(), secondDestination);
							// Round = 0, CurrentPlayer = BLUE
							playerMoveCount++;
							onMoveMade();

							player.tickets().replace(move.firstMove().ticket(), player.tickets().get(move.firstMove().ticket()) - 1);
							player.location(move.firstMove().destination());
							roundCounter++;
							currentMove = new TicketMove(player.colour(), move.firstMove().ticket(), firstDestination);
							// Round = 1, CurrentPlayer = BLUE
							onRoundStarted();
							onMoveMade();

							player.tickets().replace(move.secondMove().ticket(), player.tickets().get(move.secondMove().ticket()) - 1);
							player.location(move.finalDestination());
							roundCounter++;
							currentMove = new TicketMove(player.colour(), move.secondMove().ticket(), secondDestination);
							// Round = 2, CurrentPlayer = BLUE
							onRoundStarted();
							onMoveMade();
						}
					}
                }
            };
            // Execute the player logic
            for (ScotlandYardPlayer player : players) {
                if (player.colour() == move.colour()) {
                    if (move.colour().isDetective()) {
                    	if (move.getClass().equals(PassMove.class)) playerMoveCount ++;
                    	else move.visit(ticketMoveVisitor);
					}
                    if (move.colour().isMrX()) {
                        if (move.getClass().equals(TicketMove.class)) move.visit(ticketMoveVisitor);
                        if (move.getClass().equals(DoubleMove.class)) move.visit(doubleMoveVisitor);
                    }
					if (players.size() <= playerMoveCount) playerMoveCount = 0;
                }
            }
        }
	}

	// Get the colour of player on a node
	private Colour getPlayerOnNode (Integer node){
		for (ScotlandYardPlayer p : players){
			if (p.location() == node){
				return p.colour();
			}
		}
		return null;
	}

	// Get the set of valid moves for the current player
	private Set<Move> validMove(Colour player) {
		Set<Move> validMoves = new HashSet<>();
		for (ScotlandYardPlayer p : players){
		    if (p.colour() == player){
                Collection<Edge<Integer, Transport>> edges = new HashSet<>(graph.getEdgesFrom(graph.getNode(p.location())));

                if (player.isDetective()) {
                    for (Edge<Integer, Transport> edge : edges) {
                        if ((getPlayerOnNode(edge.destination().value()) == null
                                || getPlayerOnNode(edge.destination().value()) == BLACK)
                                && p.tickets().get(Ticket.fromTransport(edge.data())) != 0)
                            validMoves.add(new TicketMove(p.colour(), Ticket.fromTransport(edge.data()), edge.destination().value()));
                        // When a detective has no ticket, a Pass move ticket should be added in the valid moves
                        else if (p.tickets().get(BUS) + p.tickets().get(TAXI) + p.tickets().get(UNDERGROUND) == 0) {
                            validMoves.add(new PassMove(p.colour()));
                            return validMoves;
                        }
                    }
                    // When a detective has no edge to use his all tickets, a Pass move ticket should be added in
                    // valid moves
                    if (validMoves.isEmpty()) {
                        validMoves.add(new PassMove(p.colour()));
                    }
                }

                if (player.isMrX()) {
                    for (Edge<Integer, Transport> edge : edges) {
                        // When mrX can make a ticket move
                        if (getPlayerOnNode(edge.destination().value()) == null &&
                                p.tickets().get(Ticket.fromTransport(edge.data())) != 0) {
                            validMoves.add(new TicketMove(p.colour(), Ticket.fromTransport(edge.data()), edge.destination().value()));
                        }
                        // When mrX can make a secret move
                        if (getPlayerOnNode(edge.destination().value()) == null &&
                                p.tickets().get(SECRET) != 0) {
                            validMoves.add(new TicketMove(p.colour(), SECRET, edge.destination().value()));
                        }
                        if (p.tickets().get(DOUBLE) != 0 &&
                                p.tickets().get(BUS) + p.tickets().get(TAXI) + p.tickets().get(UNDERGROUND) + p.tickets().get(SECRET) >= 2 &&
                                rounds.size() - getCurrentRound() >= 2) {
                            TicketMove firstMove;
                            TicketMove secondMove;
                            //Circumstance of firstMove not using SECRET ticket
                            if (getPlayerOnNode(edge.destination().value()) == null &&
                                    p.tickets().get(Ticket.fromTransport(edge.data())) != 0) {
                                firstMove = new TicketMove(p.colour(), Ticket.fromTransport(edge.data()), edge.destination().value());
                                Collection<Edge<Integer, Transport>> doubleEdges = new HashSet<>(graph.getEdgesFrom(edge.destination()));
                                for (Edge<Integer, Transport> doubleEdge : doubleEdges){
                                    if ((getPlayerOnNode(doubleEdge.destination().value()) == null ||
                                            getPlayerOnNode(doubleEdge.destination().value()) == BLACK) &&
                                            p.tickets().get(Ticket.fromTransport(doubleEdge.data())) != 0 &&
                                            (!Ticket.fromTransport(edge.data()).equals(Ticket.fromTransport(doubleEdge.data())) ||
                                                    (Ticket.fromTransport(edge.data()).equals(Ticket.fromTransport(doubleEdge.data())) &&
                                                            p.tickets().get(Ticket.fromTransport(doubleEdge.data())) >= 2
                                                    ))){
                                        secondMove = new TicketMove(p.colour(), Ticket.fromTransport(doubleEdge.data()), doubleEdge.destination().value());
                                        validMoves.add(new DoubleMove(p.colour(), firstMove, secondMove));
                                    }
                                    if ((getPlayerOnNode(doubleEdge.destination().value()) == null ||
                                            getPlayerOnNode(doubleEdge.destination().value()) == BLACK) &&
                                            p.tickets().get(SECRET) != 0 &&
                                            !Transport.FERRY.equals(doubleEdge.data())) {
                                        secondMove = new TicketMove(p.colour(), SECRET, doubleEdge.destination().value());
                                        validMoves.add(new DoubleMove(p.colour(), firstMove, secondMove));
                                    }
                                }
                            }
                            //Circumstance of first move using SECRET ticket
                            if (getPlayerOnNode(edge.destination().value()) == null &&
                                    p.tickets().get(SECRET) != 0) {
                                firstMove = new TicketMove(p.colour(), SECRET, edge.destination().value());
                                Collection<Edge<Integer, Transport>> doubleEdges = new HashSet<>(graph.getEdgesFrom(edge.destination()));
                                for (Edge<Integer, Transport> doubleEdge : doubleEdges) {
                                    if ((getPlayerOnNode(doubleEdge.destination().value()) == null ||
                                            getPlayerOnNode(doubleEdge.destination().value()) == BLACK) &&
                                            p.tickets().get(Ticket.fromTransport(doubleEdge.data())) != 0) {
                                        secondMove = new TicketMove(p.colour(), Ticket.fromTransport(doubleEdge.data()), doubleEdge.destination().value());
                                        validMoves.add(new DoubleMove(p.colour(), firstMove, secondMove));
                                    }
                                    if ((getPlayerOnNode(doubleEdge.destination().value()) == null ||
                                            getPlayerOnNode(doubleEdge.destination().value()) == BLACK) &&
                                            p.tickets().get(SECRET) != 0 &&
                                            !Transport.FERRY.equals(doubleEdge.data())) {
                                        secondMove = new TicketMove(p.colour(), SECRET, doubleEdge.destination().value());
                                        validMoves.add(new DoubleMove(p.colour(), firstMove, secondMove));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
		return validMoves;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (spectators.contains(spectator)) throw new IllegalArgumentException();
		spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (!spectators.contains(spectator)) throw new IllegalArgumentException();
		spectators.remove(spectator);
	}

	@Override
	public void startRotate() {
		if (isGameOver() && getCurrentRound() == NOT_STARTED) throw new IllegalStateException("Game over!");
		players.get(0).player().makeMove(this, players.get(0).location(), validMove(players.get(0).colour()), this);
        for (ScotlandYardPlayer player : players){
        	if (player.colour().isDetective()) {
				if (player.colour() == getCurrentPlayer()) {
					player.player().makeMove(this, player.location(), validMove(player.colour()), this);
					if (isGameOver()) for (Spectator spectator : spectators) spectator.onGameOver(this, getWinningPlayers());
				}
			}
        }
        if (playerMoveCount == 0) onRotationComplete();
	}

	public void onMoveMade(){
	    for (Spectator spectator : spectators) spectator.onMoveMade(this, currentMove);
    }

    public void onRoundStarted(){
	    for (Spectator spectator : spectators) spectator.onRoundStarted(this, getCurrentRound());
    }

    public void onRotationComplete(){
		for (Spectator spectator : spectators) spectator.onRotationComplete(this);
    }

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> coloursOfPlayers = new ArrayList<>();
		for (PlayerConfiguration configuration : configurations) {
		     coloursOfPlayers.add(configuration.colour);
		}
		return Collections.unmodifiableList(coloursOfPlayers);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return emptySet();
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		for (ScotlandYardPlayer player : players) {
			if (player.colour() == colour) {
				if (player.colour().isMrX()) {
					if (roundCounter == NOT_STARTED) return Optional.of(0);
					if (getRounds().get(getCurrentRound() - 1)) mrXLocation = player.location();
					return Optional.of(mrXLocation);
				} else return Optional.of(player.location());
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (ScotlandYardPlayer player : players) {
			if (player.colour() == colour) {
				return Optional.of(player.tickets().get(ticket));
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		Boolean allDetectivesCannotMove = true;
		if (roundCounter >= rounds.size()) return true;
		for (ScotlandYardPlayer p : players) {
			if (p.colour().isDetective()) {
				Set<Move> passMove= new HashSet<>();
				passMove.add(new PassMove(p.colour()));
				allDetectivesCannotMove = allDetectivesCannotMove && validMove(p.colour()).equals(passMove);
				if (p.location() == players.get(0).location()) return true;
			} else if (validMove(BLACK).isEmpty()) return true;
		}
		return allDetectivesCannotMove;
	}

	@Override
	public Colour getCurrentPlayer() {
		return players.get(playerMoveCount).colour();
	}

	@Override
	public int getCurrentRound() {
		return roundCounter;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
        return new ImmutableGraph<>(graph);
	}
}
