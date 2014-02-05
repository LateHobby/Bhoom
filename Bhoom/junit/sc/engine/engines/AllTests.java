package sc.engine.engines;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    CorrectnessTests.class,
    PositionalTests.class
    
})

public class AllTests {}