package sc.util;

import java.util.Arrays;
import java.util.Random;

public class LPTTable implements TTable {

	protected int numBits;
	protected int TABLE_SIZE;
	protected int MAX_ITEMS;
	protected long MASK;

	protected long[] table;
	protected long hash;
	protected int numProbes;

	protected long cachedKey = 0;
	protected int cachedIndex = -2;

	protected int numStored = 0;

	public LPTTable(int numBits, int numProbes) {
		if (numProbes > 1) {
			throw new RuntimeException("NumProbes > 1 not supported!");
		}
		this.numBits = numBits;
		this.numProbes = numProbes;
		TABLE_SIZE = (1 << numBits);
		MAX_ITEMS = TABLE_SIZE / 2;
		table = new long[TABLE_SIZE];
		MASK = 0L;
		for (int i = 0; i < numBits - 1; i++) {
			MASK <<= 1;
			MASK |= 1L;
//			System.out.println(MASK);
		}
		// System.out.println("Table Size=" + TABLE_SIZE + " Mask=" + MASK);
		Random r = new Random(78186);
		hash = r.nextLong();
	}

	@Override
	public void store(long key, long value) {
		if (cachedKey == key) {
			table[cachedIndex + 1] = value;
			return;
		}
		boolean stored = false;
		long h = (key ^ hash);
		int index = 2 * ((int) (h & MASK));
		for (int i = 0; i < numProbes; i++) {
			if (table[index] == 0 || table[index] == key) {
				if (table[index] == 0) {
					numStored++;
				}
				table[index] = key;
				table[index + 1] = value;
				stored = true;
				break;
			}
			index += 2;
			if (index >= TABLE_SIZE) {
				index = 0;
			}
		}
		if (!stored) { // overwriting
			index -= 2;
			if (index < 0) {
				index = TABLE_SIZE -2;
			}
			table[index] = key;
			table[index + 1] = value;
		}
	}

	@Override
	public boolean contains(long key, ProbeResult returnValue) {
		long h = (key ^ hash);
		int index = 2 * ((int) (h & MASK));
		for (int i = 0; i < numProbes; i++) {
			if (table[index] == 0) {
				returnValue.existingKey = 0;
				returnValue.existingValue = 0;
				return false;
			} else {
				returnValue.existingKey = table[index];
				returnValue.existingValue = table[index+1];
			}
			if (table[index] == key) {
				cachedKey =  key;
				cachedIndex = index;
				return true;
			}
			index += 2;
			if (index >= TABLE_SIZE) {
				index = 0;
			}
		}
		return false;
	}

	@Override
	public long get(long key) {
		if (cachedKey != key) {
			throw new RuntimeException("CachedKey not equal to key");
		}
		return table[cachedIndex + 1];
	}

	public void printOccupancy() {
		System.out.printf("%d/%d\n", numStored, MAX_ITEMS);
	}

	@Override
	public int getNumStored() {
		return numStored;
	}

	@Override
	public int getCapacity() {
		return MAX_ITEMS;
	}

	public void reset() {
		Arrays.fill(table, 0);
		
	}
}
