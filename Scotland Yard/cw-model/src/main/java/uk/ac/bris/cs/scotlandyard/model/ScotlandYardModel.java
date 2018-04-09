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
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.fromTransport;

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

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
	private ArrayList<ScotlandYardPlayer> players = new ArrayList<>();
	private ArrayList<Colour> playerColours = new ArrayList<>();
	private Integer playerMoveCount = 0;

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

		for (PlayerConfiguration player : configurations){
		    ScotlandYardPlayer p = new ScotlandYardPlayer(
		            player.player,
                    player.colour,
		            player.location,
                    player.tickets
            );
		    players.add(p);
		    //For getCurrentplayer
		    playerColours.add(player.colour);
        }

	}

	@Override
    public void accept(Move move){

	}


	private Colour getPlayerOnNode (Integer node){
		for (ScotlandYardPlayer p : players){
			if (p.location() == node){
				return p.colour();
			}
		}
		return null;
	}

    private Set<Move> validMove(Colour player) {

        Set<Move> moves = new HashSet<>();
        Collection<Edge<Integer,Transport>> edges = new HashSet<>();

		for (ScotlandYardPlayer p : players) {
			if (p.colour() == player && p.isDetective()) {
				edges.addAll(graph.getEdgesFrom(graph.getNode(p.location())));
				for (Edge<Integer, Transport> edge : edges) {
					if (p.isDetective())
						if ((getPlayerOnNode(edge.destination().value()) == null
								|| getPlayerOnNode(edge.destination().value()) == BLACK)
								&& p.tickets().get(Ticket.fromTransport(edge.data())) != 0)
							moves.add(new TicketMove(player, Ticket.fromTransport(edge.data()), edge.destination().value()));
					if (p.isMrX()){
						if (getPlayerOnNode(edge.destination().value()) == null
								&& p.tickets().get(Ticket.fromTransport(edge.data())) != 0)
							moves.add(new TicketMove(player, Ticket.fromTransport(edge.data()), edge.destination().value()));
						if (p.tickets().get(DOUBLE) != 0) {
							Collection<Edge<Integer, Transport>> doubleEdges = new HashSet<>(graph.getEdgesFrom(edge.destination()));
							for (Edge<Integer, Transport> doubleEdge : doubleEdges){
								if (getPlayerOnNode(doubleEdge.destination().value()) == null){
									moves.add(new DoubleMove(player,
											Ticket.fromTransport(edge.data()),
											edge.destination().value(),
											Ticket.fromTransport(doubleEdge.data()),
											doubleEdge.destination().value()));
								}
							}
						}
					}

				}
			}
		}
		System.out.println(moves);
        return moves;
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
		for (PlayerConfiguration configuration : configurations) {
			if (configuration.colour == colour) {
				if (configuration.colour == BLACK) {
					return Optional.of(0);
				}else return Optional.of(configuration.location);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (PlayerConfiguration configuration : configurations) {
			if (configuration.colour == colour) {
				return Optional.of(configuration.tickets.get(ticket));
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
		if (playerMoveCount <= configurations.size()) {
			return playerColours.get(playerMoveCount);
		}
		else {
			playerMoveCount = 0;
			return BLACK;
		}
	}

	@Override
	public int getCurrentRound() {
		return NOT_STARTED;
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
