package stsc.algorithms.eod.primitive;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import stsc.algorithms.AlgorithmSettingImpl;
import stsc.algorithms.EodPosition;
import stsc.common.BadSignalException;
import stsc.common.Day;
import stsc.common.Side;
import stsc.common.algorithms.AlgorithmSetting;
import stsc.common.algorithms.BadAlgorithmException;
import stsc.common.algorithms.EodAlgorithm;
import stsc.common.algorithms.EodAlgorithmInit;
import stsc.common.signals.EodSignal;
import stsc.common.signals.Signal;
import stsc.common.signals.SignalsSerie;
import stsc.common.signals.StockSignal;
import stsc.signals.DoubleSignal;

/*
 * PositionNDayMStocks open position for n days on m stocks in long and short sides
 * 
 */
public class PositionNDayMStocks extends EodAlgorithm {

	private final AlgorithmSetting<Integer> n = new AlgorithmSettingImpl<Integer>(22);
	private final AlgorithmSetting<Integer> m = new AlgorithmSettingImpl<Integer>(2);
	private final AlgorithmSetting<Double> ps = new AlgorithmSettingImpl<Double>(100000.0);
	private final String factorExecutionName;

	private class Factor implements Comparable<Factor> {
		Double factor;
		String stockName;

		public Factor(Double factor, String stockName) {
			super();
			this.factor = factor;
			this.stockName = stockName;
		}

		public int compareTo(Factor o) {
			return factor.compareTo(o.factor);
		}

		@Override
		public String toString() {
			DecimalFormat decimalFormatter = new DecimalFormat("#0.00");
			return stockName + ":" + decimalFormatter.format(factor);
		}
	}

	private final HashMap<String, EodPosition> shortPositions = new HashMap<>();
	private final HashMap<String, EodPosition> longPositions = new HashMap<>();
	private Date openDate;
	private Date lastDate;

	public PositionNDayMStocks(EodAlgorithmInit init) throws BadAlgorithmException {
		super(init);
		init.getSettings().get("n", n);
		init.getSettings().get("ps", ps);
		init.getSettings().get("m", m);
		lastDate = init.getSettings().getPeriod().getTo();
		final List<String> subExecutions = init.getSettings().getSubExecutions();
		if (subExecutions.size() < 1)
			throw new BadAlgorithmException("CrossSignal algorithm should receive one stock based execution with Double");
		factorExecutionName = subExecutions.get(0);
	}

	@Override
	public void process(final Date date, final HashMap<String, Day> datafeed) throws BadSignalException {
		if (new LocalDate(date).plusDays(10).isAfter(new LocalDate(lastDate))) {
			close();
			openDate = null;
		} else if (longPositions.isEmpty()) {
			open(date, datafeed);
		} else {
			if (openDate != null && new LocalDate(openDate).plusDays(n.getValue()).isBefore(new LocalDate(date))) {
				reopen(date, datafeed);
			}
		}
	}

	private ArrayList<Factor> getSortedStocks(final Date date, final HashMap<String, Day> datafeed) {
		final ArrayList<Factor> sortedStocks = new ArrayList<>();
		for (Map.Entry<String, Day> i : datafeed.entrySet()) {
			String stockName = i.getKey();
			Signal<? extends StockSignal> signal = getSignal(stockName, factorExecutionName, date);
			if (signal != null && signal.getSignal(DoubleSignal.class) != null)
				sortedStocks.add(new Factor(signal.getSignal(DoubleSignal.class).value, stockName));
		}
		Collections.sort(sortedStocks);
		return sortedStocks;
	}

	private void reopen(final Date date, final HashMap<String, Day> datafeed) {
		close();
		open(date, datafeed);
	}

	private void close() {
		for (Map.Entry<String, EodPosition> i : shortPositions.entrySet()) {
			final EodPosition p = i.getValue();
			broker().sell(i.getKey(), Side.SHORT, p.getSharedAmount());
		}
		for (Map.Entry<String, EodPosition> i : longPositions.entrySet()) {
			final EodPosition p = i.getValue();
			broker().sell(i.getKey(), Side.LONG, p.getSharedAmount());
		}
		shortPositions.clear();
		longPositions.clear();
	}

	private void open(final Date date, final HashMap<String, Day> datafeed) {
		final ArrayList<Factor> sortedStocks = getSortedStocks(date, datafeed);

		if (sortedStocks.size() < m.getValue() * 2) {
			return;
		}

		for (int i = 0; i < m.getValue(); ++i) {
			final String stockName = sortedStocks.get(i).stockName;
			final double price = datafeed.get(stockName).getPrices().getOpen();
			final int sharesAmount = (int) (ps.getValue() / price);
			broker().buy(sortedStocks.get(i).stockName, Side.SHORT, sharesAmount);
			shortPositions.put(stockName, new EodPosition(stockName, Side.SHORT, sharesAmount));
		}
		for (int i = sortedStocks.size() - m.getValue(); i < sortedStocks.size(); ++i) {
			final String stockName = sortedStocks.get(i).stockName;
			final double price = datafeed.get(stockName).getPrices().getOpen();
			final int sharesAmount = (int) (ps.getValue() / price);
			broker().buy(sortedStocks.get(i).stockName, Side.LONG, sharesAmount);
			longPositions.put(stockName, new EodPosition(stockName, Side.LONG, sharesAmount));
		}
		openDate = date;
	}

	@Override
	public SignalsSerie<EodSignal> registerSignalsClass(EodAlgorithmInit init) throws BadAlgorithmException {
		return null;
	}
}