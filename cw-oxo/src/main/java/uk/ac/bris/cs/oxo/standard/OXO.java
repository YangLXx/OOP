package uk.ac.bris.cs.oxo.standard;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import uk.ac.bris.cs.gamekit.matrix.ImmutableMatrix;
import uk.ac.bris.cs.gamekit.matrix.Matrix;
import uk.ac.bris.cs.gamekit.matrix.SquareMatrix;
import uk.ac.bris.cs.oxo.Cell;
import uk.ac.bris.cs.oxo.Outcome;
import uk.ac.bris.cs.oxo.Player;
import uk.ac.bris.cs.oxo.Side;
import uk.ac.bris.cs.oxo.Spectator;

/**
 * sample implementation of the OXO model
 */
public class OXO implements OXOGame, Consumer<Move> {

	private final SquareMatrix<Cell> matrix;
	private Player noughtSide, crossSide;
	private final List<Spectator> spectators = new CopyOnWriteArrayList<>();
	private Side currentSide;
	private Set<Move> moves;

	public OXO(int size, Side startSide, Player noughtSide, Player crossSide) {
		this.matrix = new SquareMatrix<>(size, new Cell());
		this.currentSide = requireNonNull(startSide);
		this.noughtSide = requireNonNull(noughtSide);
		this.crossSide = requireNonNull(crossSide);
	}

	@Override
	public void registerSpectators(Spectator... spectators) {
		this.spectators.addAll(Arrays.asList(spectators));
	}

	@Override
	public void unregisterSpectators(Spectator... spectators) {
		this.spectators.removeAll(Arrays.asList(spectators));
	}

	@Override
	public void start() {
		requestMove(currentPlayer());
	}

	private void requestMove(Player player) {
		moves = validMoves();
		player.makeMove(this, moves, this);
	}

	@Override
	public void accept(Move move) {
		if (!moves.contains(move)) throw new IllegalArgumentException("Invalid move");
		matrix.put(move.row, move.column, new Cell(currentSide));
		for (Spectator spectator : spectators) {
			spectator.moveMade(currentSide, move);
		}
		if (straightLineFormed(currentSide)) {
			notifyGameOver(new Outcome(currentSide));
		} else if (noEmptyCells()) {
			notifyGameOver(new Outcome());
		} else {
			currentSide = currentSide.other();
			requestMove(currentPlayer());
		}
	}

	private void notifyGameOver(Outcome outcome) {
		for (Spectator spectator : spectators) {
			spectator.gameOver(outcome);
		}
	}

	private Set<Move> validMoves() {
		Set<Move> moves = new HashSet<>();
		for (int row = 0; row < matrix.rowSize(); row++) {
			for (int col = 0; col < matrix.columnSize(); col++) {
				if (matrix.get(row, col).isEmpty()) moves.add(new Move(row, col));
			}
		}
		return Collections.unmodifiableSet(moves);
	}

	private boolean noEmptyCells() {
		for (Cell cell : matrix.asList())
			if (cell.isEmpty()) return false;
		return true;
	}

	private boolean straightLineFormed(Side side) {
		for (int i = 0; i < matrix.columnSize(); i++) {
			if (onSameSide(side, matrix.row(i))) return true;
			if (onSameSide(side, matrix.column(i))) return true;
		}
		if (onSameSide(side, matrix.mainDiagonal())) return true;
		if (onSameSide(side, matrix.antiDiagonal())) return true;
		return false;
	}

	private boolean onSameSide(Side side, List<Cell> cells) {
		for (Cell cell : cells)
			if (!cell.sameSideAs(side)) return false;
		return true;
	}

	private Player currentPlayer() {
		return currentSide == Side.CROSS ? crossSide : noughtSide;
	}

	@Override
	public Matrix<Cell> board() {
		return new ImmutableMatrix<>(matrix);
	}

	@Override
	public Side currentSide() {
		return currentSide;
	}
}