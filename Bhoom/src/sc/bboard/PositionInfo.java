package sc.bboard;

import java.io.Serializable;

public class PositionInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8056671305910572696L;
	
	public OneSidePositionInfo wConfig = new OneSidePositionInfo(true);
	public OneSidePositionInfo bConfig = new OneSidePositionInfo(false);

	// Should be called after individual occupancies have been updated
	public void updateForEval() {
		wConfig.updateOccupancy();
		bConfig.updateOccupancy();
		// update attacks
		wConfig.updateAttacks(bConfig);
		bConfig.updateAttacks(wConfig);
		// update king in check flag
		wConfig.updateKingInCheck(bConfig.all_attacks);
		bConfig.updateKingInCheck(wConfig.all_attacks);
	}

	public void updateDerivedBoards() {
		updateForEval();
		// update pins and checks
		wConfig.updatePinsAndChecks(bConfig);
		bConfig.updatePinsAndChecks(wConfig);
	}

	public void setTo(PositionInfo other) {
		wConfig.setTo(other.wConfig);
		bConfig.setTo(other.bConfig);
		
	}

	public boolean drawByInsufficientMaterial() {
		return !(wConfig.hasMaterialToWin || bConfig.hasMaterialToWin);
	}
}
