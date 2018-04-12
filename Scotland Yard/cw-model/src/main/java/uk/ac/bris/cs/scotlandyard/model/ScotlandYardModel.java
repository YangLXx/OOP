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
                    for (ScotlandYardPlayer player : players) {
                        if (player.colour() == move.colour()) {
                            player.tickets().replace(move.ticket(), player.tickets().get(move.ticket()) - 1);
                            player.location(move.destination());
                            if(player.isDetective()) {
                                players.get(0).tickets().replace(move.ticket(), players.get(0).tickets().get(move.ticket()) + 1);
                            }
                        }
                    }
                }
            };
            //Override visit method for DoubleMove from MoveVisitor interface
            MoveVisitor doubleMoveVisitor = new MoveVisitor() {
                @Override
                public void visit(DoubleMove move) {
                    for (ScotlandYardPlayer player : players) {
                        if (player.colour() == move.colour()) {
                            player.tickets().replace(DOUBLE,player.tickets().get(DOUBLE) - 1);
                            player.tickets().replace(move.firstMove().ticket(), player.tickets().get(move.firstMove().ticket()) - 1);
                            player.location(move.firstMove().destination());
                            roundCounter++;
                            player.tickets().replace(move.secondMove().ticket(), player.tickets().get(move.secondMove().ticket()) - 1);
                            player.location(move.finalDestination());
                        }
                    }
                }
            };
            // Execute the player logic
            for (ScotlandYardPlayer player : players) {
                if (player.colour() == move.colour()) {
                    if (move.colour().isDetective() && !move.getClass().equals(PassMove.class))
                        move.visit(ticketMoveVisitor);
                    if (move.colour().isMrX()) {
                        if (move.getClass().equals(TicketMove.class)) move.visit(ticketMoveVisitor);
                        if (move.getClass().equals(DoubleMove.class)) move.visit(doubleMoveVisitor);
                        System.out.println(roundCounter);
                    }
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
                        // When a detective has no ticket, a PASSMOVE ticket should be added in the valid moves
                        else if (p.tickets().get(BUS) + p.tickets().get(TAXI) + p.tickets().get(UNDERGROUND) == 0) {
                            validMoves.add(new PassMove(p.colour()));
                            return validMoves;
                        }
                    }
                    // When a detective has no edge to use his all tickets, a PASSMOVE ticket should be added in
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
                                rounds.size() - roundCounter >= 2) {
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

	}

	@Override
	public void unregisterSpectator(Spectator spectator) {

	}

	@Override
	public void startRotate() {
        for (ScotlandYardPlayer player : players){
            if (player.colour() == getCurrentPlayer() ) {
				player.player().makeMove(this, player.location(), validMove(player.colour()), this);
				playerMoveCount++;
			}
        }
        playerMoveCount = 0;
	}

    public void onRotationComplete(){
    }

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
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
				if (player.colour() == BLACK) {
					return Optional.of(0);
				}else return Optional.of(player.location());
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
		return false;
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
