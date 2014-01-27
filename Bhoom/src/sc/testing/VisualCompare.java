package sc.testing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;

public class VisualCompare extends JFrame {

	
	DirList leftList;
	DirList rightList;
	JTextArea ta = new JTextArea();
	JButton compareButton = new JButton("Compare");
	
	public VisualCompare() {
		super("Directory: " + SaveBenchmark.benchmark.getAbsolutePath());
		leftList = new DirList(SaveBenchmark.benchmark);
		rightList = new DirList(SaveBenchmark.benchmark);
		setLayout(new BorderLayout());
		JPanel pan = new JPanel();
		pan.setLayout(new GridLayout(1,2,2,4));
		pan.add(getScrollPane(leftList));
		pan.add(getScrollPane(rightList));
		add(pan, BorderLayout.WEST);
		JPanel bottomPan = new JPanel();
		bottomPan.add(compareButton);
		add(bottomPan, BorderLayout.SOUTH);
		JScrollPane scp = new JScrollPane(ta);
		scp.setPreferredSize(new Dimension(400, 400));
		add(scp, BorderLayout.CENTER);
		
		ta.setFont(new Font("Courier", 12, 12));
		compareButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				File oldf = leftList.getSelectedValue();
				File newf = rightList.getSelectedValue();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(baos);
				
				try {
					CompareBenchmarks.printComparison(ps, oldf, newf);
					CompareBenchmarks.printIndividual(ps, newf);
					CompareBenchmarks.printIndividual(ps, oldf);
					ta.setText(new String(baos.toByteArray()));
				} catch (ClassNotFoundException
						| IOException e) {
					e.printStackTrace();
				}
				
			}
		});
	}
	
	private Component getScrollPane(DirList list) {
		JScrollPane scp = new JScrollPane(list);
		scp.setPreferredSize(new Dimension(300, 300));
		return scp;
	}

	private class DirList extends JList<File> {
		
		DirList(File dir) {
			DefaultListModel<File> lm = new DefaultListModel<File>();
			setModel(lm);
			
			for (File file : dir.listFiles()) {
				if (file.isFile() /*&& file.getName().endsWith(".ser") */) {
					lm.addElement(file);
				}
			}
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setCellRenderer(new DefaultListCellRenderer() {

				@Override
				public Component getListCellRendererComponent(JList<?> arg0,
						Object arg1, int arg2, boolean arg3, boolean arg4) {
					File f = (File) arg1;
					return super.getListCellRendererComponent(arg0, f.getName(), arg2, arg3, arg4);
				}

			
				
			});
		}
	}
	
	public static void main(String[] args) {
		VisualCompare vc = new VisualCompare();
		vc.pack();
		vc.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		vc.setVisible(true);
	}
}
