package stsc.algorithms.privitive;

import java.util.HashMap;
import java.util.Date;

import stsc.algorithms.EodAlgorithm;
import stsc.algorithms.primitive.TestingEodAlgorithm;
import stsc.common.Day;
import stsc.signals.BadSignalException;
import stsc.testhelper.TestHelper;
import junit.framework.TestCase;

public class SimpleTradingAlgorithmTest extends TestCase {
	public void testTestingEodAlgorithm() throws BadSignalException {

		final EodAlgorithm.Init init = TestHelper.getEodAlgorithmInit();

		TestingEodAlgorithm tea = new TestingEodAlgorithm(init);
		tea.process(new Date(), new HashMap<String, Day>());
	}
}