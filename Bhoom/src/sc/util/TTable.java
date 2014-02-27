package sc.util;

public interface TTable {
	
		static public final int EXACT = 1;
		static public final int UPPERBOUND = 2;
		static public final int LOWERBOUND = 3;
 
       void store(long key, long value);
      
       boolean contains(long key, ProbeResult returnValue);
      
       long get(long key);
      
       int getNumStored();
      
       int getCapacity();
       
       static public class ProbeResult {
    	   public long existingKey;
    	   public long existingValue;
       }
       
       static public class TTEntry {
    	   public int type;
    	   public int eval;
    	   public int depthLeft;
    	   public int move;
       }
      
}