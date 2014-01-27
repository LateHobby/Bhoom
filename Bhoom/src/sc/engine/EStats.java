package sc.engine;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

public class EStats implements Serializable {

	private static final long serialVersionUID = -875265102242669182L;

	public StatsPair perIter = new StatsPair();
	public StatsPair cumulative = new StatsPair();
	
	public void addNode(boolean quiescent) {
		perIter.addNode(quiescent);
		cumulative.addNode(quiescent);
	}
	
	public void ttHit(boolean quiescent) {
		perIter.ttHit(quiescent);
		cumulative.ttHit(quiescent);
	}
	
	public void betaCutoff(int index, boolean quiescent) {
		perIter.betaCutoff(index, quiescent);
		cumulative.betaCutoff(index, quiescent);
	}
	
	public void nullCutoff() {
		perIter.nullCutoff();
		cumulative.nullCutoff();
		
	}
	
	public void tryNull() {
		perIter.tryNull();
		cumulative.tryNull();
		
	}
	public void alphaImprovement(int index) {
		perIter.alphaImprovement(index);
		cumulative.alphaImprovement(index);
	}
	
	public void depth(int d) {
		perIter.depth(d);
		cumulative.depth(d);
	}
	
	private class Stats implements Serializable {
		
		private static final long serialVersionUID = -3919053195956411655L;

		String name;
		
		int nodes;
		int betaCutoffs;
		int sumCutoffIndices;
		int ttHits;
		int [] alphaImprovementCounts = new int[5];

		int nullCutoffs;

		int nullsTried;
		
		Stats(String name) {
			this.name = name;
		}
		void reset() {
			nodes = 0;
			betaCutoffs = 0;
			nullCutoffs = 0;
			sumCutoffIndices = 0;
			ttHits = 0;
			for (int i = 0; i < alphaImprovementCounts.length; i++) {
				alphaImprovementCounts[i] = 0;
			}
		}

		public void betaCutoff(int index) {
			betaCutoffs++;
			sumCutoffIndices += index;
		}

		public void alphaImprovement(int index) {
			if (index <= 4) {
				alphaImprovementCounts[index]++;
			} else {
				alphaImprovementCounts[4]++;
			}
		}
		
		public String getString() {
			StringWriter sw = new StringWriter();
			PrintWriter ps = new PrintWriter(sw);
			ps.printf("%s  Nodes: %d TTHits: %d%% BCuts: %d%% AvgBCut: %3.2f NullC:[%d, %d]", 
					name, nodes, (ttHits * 100) /(nodes+1), (betaCutoffs * 100)/(nodes + 1), (double) sumCutoffIndices/(betaCutoffs + 1), 
					nullsTried, nullCutoffs);
			ps.printf(" AlphaI[");
			for (int i = 0; i < alphaImprovementCounts.length; i++) {
				ps.printf("%d, ", alphaImprovementCounts[i]);
			}
			ps.printf("]");
			return sw.toString();
		}
		public void nullCutoff() {
			nullCutoffs++;
			
		}
		public void tryNull() {
			nullsTried++;
			
		}

	}
	
	public class StatsPair implements EngineStats, Serializable {
		private static final long serialVersionUID = -2624499189674713575L;
		Stats stats = new Stats("NonQ");
		Stats qstats = new Stats("Quiesct");
		int depth;
		
		
		@Override
		public int getNodes(boolean quiescent) {
			return quiescent ? qstats.nodes : stats.nodes;
		}
		
		public void tryNull() {
			stats.tryNull();
			
		}

		public void nullCutoff() {
			stats.nullCutoff();
			
		}

		public void depth(int d) {
			this.depth = d;
			
		}

		public void betaCutoff(int index, boolean quiescent) {
			Stats s = quiescent ? qstats : stats;
			s.betaCutoff(index);
			
		}

		public void alphaImprovement(int index) {
			stats.alphaImprovement(index);
		}
		
		public void ttHit(boolean quiescent) {
			Stats s = quiescent ? qstats : stats;
			s.ttHits++;
			
		}

		public void addNode(boolean quiescent) {
			Stats s = quiescent ? qstats : stats;
			s.nodes++;
			
		}

		@Override
		public double getAvgBetaCutoffIndex(boolean quiescent) {
			Stats s = quiescent ? qstats : stats;
			return ((double) s.sumCutoffIndices/s.betaCutoffs);
		}
		@Override
		public double getTTHitPercentage(boolean quiescent) {
			Stats s = quiescent ? qstats : stats;
			return (s.ttHits * 100)/s.nodes;
		}

		public void reset() {
			stats.reset();
			qstats.reset();
			
		}
		
		public String getStatsString() {
			return stats.getString() + "\n" + qstats.getString();
			
		}

		@Override
		public int getDepth() {
			return depth;
		}
		
	}

	

	
	
	
}
