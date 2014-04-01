package stsc.algorithms;

import stsc.trading.Side;

public class EodPosition {
	final String stockName;
	final Side side;
	int sharedAmount;

	public int getSharedAmount() {
		return sharedAmount;
	}

	public void setSharedAmount(int sharedAmount) {
		this.sharedAmount = sharedAmount;
	}

	public String getStockName() {
		return stockName;
	}

	public Side getSide() {
		return side;
	}

	public EodPosition(String stockName, Side side, int sharedAmount) {
		this.stockName = stockName;
		this.side = side;
		this.sharedAmount = sharedAmount;
	}
}