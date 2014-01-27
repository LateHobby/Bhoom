package sc.apps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import sc.util.PrintUtils;

public class DebugHelper {

	
	public static void main(String[] args) {
		JFrame fr = new JFrame("DebugHelper");
		fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fr.setLayout(new BorderLayout());
		final JTextField tp = new JTextField(25);
		final JTextArea ta = new JTextArea();
		JScrollPane scp = new JScrollPane(ta);
		scp.setPreferredSize(new Dimension(200, 300));
		fr.add(tp, BorderLayout.NORTH);
		fr.add(scp, BorderLayout.CENTER);
		tp.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				long l = Long.parseLong(tp.getText());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(baos);
				PrintUtils.printAsBoards(ps, l);
				String s = new String(baos.toByteArray());
				ta.setText(s);
			}
			
		});
		fr.pack();
		fr.setVisible(true);
	}

}
