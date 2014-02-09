package sc.engine.engines;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    MinimaxTest.class,
    AspWinFeaturesTest.class,
    AspWinVsMtdTest.class,
    MiscQualityAndPerformanceTests.class
})

public class AllTests {}