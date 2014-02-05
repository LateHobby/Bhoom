package sc.bboard;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TestEBitBoard.class,
    TestFMoves.class,
    TestPositionInfo.class
    
})

public class AllTests {}