package uk.ac.bris.cs.oxo;

/**
 * A side of the OXO game
 */
public enum Side {
	NOUGHT("O"), CROSS("X");

	private final String symbol;

	Side(String symbol) {
		this.symbol = symbol;
	}

	/**
	 * The ASCII representation of the side
	 * @return the symbol; never null
	 */
	public String symbol() {
		return symbol;
	}

	/**
	 * The other side of the current side
	 * 
	 * @return {@link #CROSS} if this side is {@link #NOUGHT} or {@link #NOUGHT}
	 *         if this side is {@link #CROSS}
	 */
	public Side other() {
		return this == NOUGHT ? CROSS : NOUGHT;
	}

}
