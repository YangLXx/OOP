package uk.ac.bris.cs.oxo;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the result of a game. Which either contains a winning side, or nothing(tie)
 */
public class Outcome implements Serializable {

	private static final long serialVersionUID = 1241278933487588044L;
	private final Side winningSide;

	/**
	 * Creates a outcome with a winning side
	 * @param winningSide the side the won; null for tie
	 */
	public Outcome(Side winningSide) {
		this.winningSide = winningSide;
	}

	/**
	 * Creates an outcome with no winner, i.e a tie
	 */
	public Outcome() {
		this(null);
	}

	/**
	 * Gets the winning side as an optional value
	 * @return the optional value where a tie is {@link Optional#empty()} ; never null
	 */
	public Optional<Side> winningSide() {
		return Optional.ofNullable(winningSide);
	}

	@Override
	public String toString() {
		return String.format("Outcome{%s}", winningSide);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Outcome outcome = (Outcome) o;
		return winningSide == outcome.winningSide;
	}

	@Override
	public int hashCode() {
		return Objects.hash(winningSide);
	}
}
