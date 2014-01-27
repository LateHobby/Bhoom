package sc.util;

/** 
 * Poolable objects should be cleaned or reset when they are returned to the pool.
 * 
 * @author Shiva
 *
 */
public interface Poolable {

	public void reset();
	
}
