package sc.engine;

import sc.bboard.FMoves;
import sc.bboard.OneSidePositionInfo;
import sc.bboard.PositionInfo;
import sc.encodings.Encodings;
import sc.engine.BaseNegamaxEngine.Moves;
import sc.util.BitManipulation;
import sc.util.ObjectPool;

public class SeeOld {

	// Logic taken from Mediocre
	public int evaluateMove(EngineBoard board, int move,
			ObjectPool<Moves> movesPool) {
		PositionInfo pinfo = board.getPositionInfo();
		OneSidePositionInfo friendly = board.getWhiteToMove() ? pinfo.wConfig
				: pinfo.bConfig;
		OneSidePositionInfo enemy = board.getWhiteToMove() ? pinfo.bConfig
				: pinfo.wConfig;
		short toSquare = Encodings.getToSquare(move);
		byte piece = board.getPiece(toSquare);
		int score = (piece == Encodings.EMPTY) ? 0 : getValue(piece);
		Moves attackerValues = movesPool.allocate();
		int numAttackers = fillAttackerValues(enemy, toSquare, attackerValues);
		Moves defenderValues = movesPool.allocate();
		int numDefenders = fillAttackerValues(friendly, toSquare, defenderValues);
		Moves scores = movesPool.allocate();
		short fromSquare = Encodings.getFromSquare(move);
		int attackedPieceValue = getValue(board.getPiece(fromSquare));
		int aindex = 0;
		scores.moves[0] = score;
		int sindex = 1;
		while (true) {
			if (aindex < numAttackers) {
				scores.moves[sindex] = attackedPieceValue
						- scores.moves[sindex - 1];
				sindex++;
				attackedPieceValue = attackerValues.moves[aindex];
			} else {
				break;
			}
			if (aindex < numDefenders) {
				scores.moves[sindex] = attackedPieceValue
						- scores.moves[sindex - 1];
				sindex++;
				attackedPieceValue = defenderValues.moves[aindex];
			} else {
				break;
			}
			aindex++;
		}

		while (sindex > 1) {
			sindex--;
			if (scores.moves[sindex - 1] > -scores.moves[sindex]) {
				scores.moves[sindex - 1] = -scores.moves[sindex];
			}
		}

		int result = scores.moves[0];
		
		movesPool.release(attackerValues);
		movesPool.release(defenderValues);
		movesPool.release(scores);
		
		return result;
	}

	private int fillAttackerValues(OneSidePositionInfo enemy, short toSquare,
			Moves attackerValues) {
		long toLoc = BitManipulation.bit_masks[toSquare];
		int num = 0;
		if ((enemy.pawn_attacks & toLoc) != 0L) {
			long capturingPawns = FMoves.capturingPawns(enemy.white, toSquare, enemy.pawn_occ);
			while (capturingPawns != 0L) {
				long pawnLoc = Long.lowestOneBit(capturingPawns);
				attackerValues.moves[num++] = 100;
				capturingPawns &= ~pawnLoc;
			}
		}
		long occ = enemy.occ_boards[4]; // knight
		while (occ != 0L) {
			long figLoc = Long.lowestOneBit(occ);
			short figSquare = (short) Long.numberOfTrailingZeros(figLoc);
			long att = enemy.figure_attacks[figSquare] & toLoc;
			if (att != 0L) {
				attackerValues.moves[num++] = 300;
			}
			occ &= ~figLoc;
		}
		occ = enemy.occ_boards[3]; // bishop
		while (occ != 0L) {
			long figLoc = Long.lowestOneBit(occ);
			short figSquare = (short) Long.numberOfTrailingZeros(figLoc);
			long att = enemy.figure_attacks[figSquare] & toLoc;
			if (att != 0L) {
				attackerValues.moves[num++] = 300;
			}
			occ &= ~figLoc;
		}
		occ = enemy.occ_boards[2]; // rook
		while (occ != 0L) {
			long figLoc = Long.lowestOneBit(occ);
			short figSquare = (short) Long.numberOfTrailingZeros(figLoc);
			long att = enemy.figure_attacks[figSquare] & toLoc;
			if (att != 0L) {
				attackerValues.moves[num++] = 500;
			}
			occ &= ~figLoc;
		}
		occ = enemy.occ_boards[2]; // queen
		if (occ != 0L) {
			long figLoc = Long.lowestOneBit(occ);
			short figSquare = (short) Long.numberOfTrailingZeros(figLoc);
			long att = enemy.figure_attacks[figSquare] & toLoc;
			if (att != 0L) {
				attackerValues.moves[num++] = 800;
			}
		}
		return num;
	}


	private int getValue(byte piece) {
		switch (piece) {
		case Encodings.WKING:
		case Encodings.BKING:
			return 3000;
		case Encodings.WQUEEN:
		case Encodings.BQUEEN:
			return 800;
		case Encodings.WROOK:
		case Encodings.BROOK:
			return 500;
		case Encodings.WBISHOP:
		case Encodings.BBISHOP:
			return 300;
		case Encodings.WKNIGHT:
		case Encodings.BKNIGHT:
			return 300;
		case Encodings.WPAWN:
		case Encodings.BPAWN:
			return 100;
		default:
			throw new RuntimeException("No piece moved?");

		}
	}

}
