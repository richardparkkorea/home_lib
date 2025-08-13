package org.xcopy2;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;
import javax.swing.JList;

import java.awt.Dimension;
import java.io.File;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;

public class frmCopying2 extends JFrame {

	frmCopying2 _this = this;

	private IoHandle2 m_rc = null;
	private JPanel contentPane;
	private JLabel lblNewLabel;
	private JPanel panel;
	private JButton btnNewButton;
	private JProgressBar progressBar;
	private JScrollPane scrollPane;
	private JList list;
	DefaultListModel model = new DefaultListModel();

	boolean m_exitOnClose = true;
	/*
	 * public void start(RmiClient rc) {
	 * 
	 * m_rc=rc;
	 * 
	 * EventQueue.invokeLater(new Runnable() { public void run() { try { CopyingFrame frame = new CopyingFrame(rc);
	 * frame.setVisible(true); } catch (Exception e) { e.printStackTrace(); } } }); }
	 */

	/**
	 * Create the frame.
	 */
	public frmCopying2(IoHandle2 rc, boolean exitOnClose) {
		m_rc = rc;

		m_exitOnClose = exitOnClose;
		if (exitOnClose) {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} else {
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}
		setBounds(100, 100, 316, 230);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		lblNewLabel = new JLabel("New label");
		lblNewLabel.setPreferredSize(new Dimension(57, 150));
		lblNewLabel.setVerticalAlignment(SwingConstants.TOP);
		contentPane.add(lblNewLabel, BorderLayout.NORTH);

		panel = new JPanel();
		contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		btnNewButton = new JButton("Exit");
		btnNewButton.addActionListener(new BtnNewButtonActionListener());
		panel.add(btnNewButton, BorderLayout.EAST);

		progressBar = new JProgressBar();
		panel.add(progressBar, BorderLayout.NORTH);

		scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		list = new JList(model);
		scrollPane.setViewportView(list);

		// my code
		setVisible(true);
		// new Thread(this).start();// start
		// setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		new Timer().schedule(new RemindTask(), 300, 300);
	}

	public static String getShortFileName(String s) {

		int fp = 15;

		int l = s.length();
		int ls = s.lastIndexOf("\\");

		if (ls > 0) {

			String front = "";
			if (ls > fp) {
				front = s.substring(0, fp);
			} else {
				front = s.substring(0, ls);
			}

			String tail = s.substring(ls, l);

			return front + "..." + tail;
		}

		return s;
	}

	class RemindTask extends TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			// try {

			// while (this.isVisible()) {

			// SwingUtilities.invokeLater(new Runnable() {

			// EventQueue.invokeLater(new Runnable() {
			// public void run() {

			String remain = "";

			try {
				long seconds = m_rc.m_entryLen / m_rc.m_speed;

				LocalTime timeOfDay = LocalTime.ofSecondOfDay(seconds);
				remain = timeOfDay.toString();

			} catch (Exception e) {

			}

			_this.setTitle(m_rc.m_srcIp + "->" + m_rc.m_destIp);

			String str = "";

			str = "<html> <font color=blue>" + m_rc.m_title+"</font><br>"//
					+getShortFileName(m_rc.m_currentFileName) + "<br>"//
					+ "copying: " + capacity(m_rc.m_curPos) + "/" + capacity(m_rc.m_curLen) + "<br>"//
					+ "total:" + capacity(m_rc.m_entryPos) + "/" + capacity(m_rc.m_entryLen) + "   speed: "
					+ capacity(m_rc.m_speed) + "  remain:" + remain + "<br>"//
					+ "file & dir :" + (m_rc.m_currentCopyingIndex) + "/" + (m_rc.m_targetFileCount) + "<br>"//
					+ (m_rc.m_successCount) + " copied  " + (m_rc.m_failCount) + " fail <br>"//

					+ "" + "</html>";

			lblNewLabel.setText(str);
			progressBar.setMaximum((int) (m_rc.m_entryLen / 1000));
			progressBar.setValue((int) (m_rc.m_entryPos / 1000));

			synchronized (m_rc.m_logs) {

				if (m_rc.m_logs.size() > 0) {
					for (int i = 0; i < m_rc.m_logs.size(); i++) {
						DefaultListModel model = (DefaultListModel) list.getModel();
						model.addElement(m_rc.m_logs.get(0));
						m_rc.m_logs.remove(0);

					}
					list.ensureIndexIsVisible(model.size() - 1);
				}
			}

			// }
			// });

			// Thread.sleep(100);

		}
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }

	}

	private class BtnNewButtonActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			int dialogButton = JOptionPane.YES_NO_OPTION;

			// dialogButton=JOptionPane.showConfirmDialog (null, "Would you like to exit?","Warning",dialogButton);

			// if(dialogButton == JOptionPane.YES_OPTION){ //The ISSUE is here

			if (m_exitOnClose) {
				System.exit(0);
			}

			_this.dispose();
			m_rc.m_letsStop = true;

			// }
		}
	}

	static String capacity(long n) {

		double l = n;

		if (l >= (1024L * 1024L * 1024L)) {// g

			return String.format("%.3fg", (l / (1024L * 1024L * 1024L)));

		} else if (l >= (1024L * 1024L)) {// m

			return String.format("%.2fm", (l / (1024L * 1024L)));

		} else if (l >= 1024L) {// k

			return String.format("%.2fk", (l / (1024L)));

		}

		return "" + l + "b";
	}
}
