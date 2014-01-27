package sc.util;

public interface TTable {
	
		static public final int EXACT = 1;
		static public final int UPPERBOUND = 2;
		static public final int LOWERBOUND = 3;
 
       void store(long key, long value);
      
       boolean contains(long key);
      
       long get(long key);
      
       int getNumStored();
      
       int getCapacity();
       
       static public class TTEntry {
    	   public int type;
    	   public int eval;
    	   public int depthLeft;
    	   public int move;
       }
      
}