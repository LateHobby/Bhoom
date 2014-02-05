package sc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	sc.bboard.AllTests.class,
	sc.encodings.AllTests.class,
	sc.engine.AllTests.class,
	sc.engine.engines.AllTests.class,
	sc.engine.ttables.AllTests.class,
	sc.util.AllTests.class
})


public class AllTests {}

