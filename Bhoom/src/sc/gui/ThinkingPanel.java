package sc.gui;

import java.awt.BorderLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import sc.engine.EngineBoard;
import sc.util.BoardUtils;

public class ThinkingPanel extends JPanel {

	private JTable table;
	private ThinkingModel model = new ThinkingModel();
	
	public ThinkingPanel() {
		setLayout(new BorderLayout());
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(75);
		table.getColumnModel().getColumn(2).setPreferredWidth(150);
		table.getColumnModel().getColumn(3).setPreferredWidth(150);
		table.getColumnModel().getColumn(4).setPreferredWidth(75);
		table.getColumnModel().getColumn(0).setMinWidth(50);
		table.getColumnModel().getColumn(1).setMinWidth(75);
		table.getColumnModel().getColumn(2).setMinWidth(150);
		table.getColumnModel().getColumn(3).setMinWidth(150);
		table.getColumnModel().getColumn(4).setMinWidth(75);
		table.getColumnModel().getColumn(5).setMinWidth(400);
		
		JScrollPane scp = new JScrollPane(table);
		add(scp, BorderLayout.CENTER);
	}
	
	public void thinkingUpdate(EngineBoard board, String str) {
		String[] sa = str.split("\\s+");
		if (!"info".equals(sa[0])) {
			return;
		}
		RowElement re = new RowElement();
		re.eval = Integer.parseInt(sa[3]);
		re.depth = Integer.parseInt(sa[5]);
		re.nodes = Integer.parseInt(sa[7]);
		re.nodesPerSec = Integer.parseInt(sa[9]);
		re.time = Integer.parseInt(sa[11]);
		re.continuation = BoardUtils.convertToPGNString(board, sa, 13, sa.length);
		model.rows.add(re);
		
		model.fireTableDataChanged();
		
	}
	
	public void clear() {
		model.rows.clear();
		model.fireTableDataChanged();
	}
	private class ThinkingModel extends AbstractTableModel {
		String[] cols = new String[] {"Depth", "Eval", "Nodes", "Nodes/Sec", "Time", "Continuation"};
		NumberFormat nf = NumberFormat.getNumberInstance();
		
		List<RowElement> rows = new ArrayList<RowElement>();
		
		ThinkingModel() {
			nf.setMaximumFractionDigits(2);
		}
		
		
		
		@Override
		public String getColumnName(int col) {
			return cols[col];
		}



		@Override
		public int getColumnCount() {
			return cols.length;
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (row < 0 || row > rows.size()-1) {
				return null;
			}
			RowElement re = rows.get(row);
			if (re == null) {
				return null;
			}
			switch (column) {
			case 0: return re.depth;
			case 1: return nf.format(re.eval/100.0);
			case 2: return re.nodes;
			case 3: return re.nodesPerSec;
			case 4: return re.time;
			case 5: return re.continuation;
			default: return null;
			}
		}
		
	}
	
	private class RowElement {
		public int nodesPerSec;
		int depth;
		int time;
		int nodes;
		int eval;
		String continuation;
	}
}
