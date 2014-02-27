package sc.util;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TestBitManipulation.class,
    TestBoardUtils.class,
    TestFENInfo.class,
    TestLPTTable.class,
    BinaryMaxHeapTest.class
    
})

public class AllTests {}