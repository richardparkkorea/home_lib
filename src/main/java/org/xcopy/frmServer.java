package org.xcopy;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import home.lib.lang.Timer2;
import home.lib.lang.Timer2Task;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class frmServer extends JFrame {

	private JPanel contentPane;
	private JTextField txtPort;

	IoHandle m_server = null;

	JLabel lbStat;
	
	String m_ip="127.0.0.1";

	/**
	 * Create the frame.
	 */
	public frmServer(String ip) {
		m_ip=ip;
		
		
		setTitle("xcopy server");
		
		putOnTray();
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				
				if (SystemTray.isSupported()) {

					setVisible(false);

				} else {
					int response = JOptionPane.showConfirmDialog(frmServer.this, // owner
							"exit?", // message
							"confirm", // title
							JOptionPane.YES_NO_OPTION); // optionType

					if (response == JOptionPane.YES_OPTION) {

						// gDb.doShutdown();

						System.exit(0);

					}
				}
				
				
			}
		});
		
		
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 265, 267);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		//contentPane.add(panel);
		contentPane.add(panel, BorderLayout.NORTH);

		JLabel lblPort = new JLabel("port");
		panel.add(lblPort);

		txtPort = new JTextField();
		panel.add(txtPort);
		txtPort.setText("8475");
		txtPort.setColumns(10);

		JButton btnStart = new JButton("Start");
		panel.add(btnStart);

		lbStat = new JLabel("New label");
		contentPane.add(lbStat, BorderLayout.CENTER);
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				stop();

				start();

			}
		});

		

		new Timer2().schedule(new Timer2Task() {

			@Override
			public void start(Timer2 tmr) {

				
				

			}

			@Override
			public void run(Timer2 tmr) {
				updateStat();
			}

			@Override
			public void stop(Timer2 tmr) {

			}

		}, 100, 1000);
		
		
		start();
		
		
	}

	void start() {

		try {
			int port = Integer.valueOf(txtPort.getText());

			m_server = new IoHandle(m_ip, port, true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e);
		}

	}

	void stop() {
		try {
			m_server.close();
		} catch (Exception e) {

		}
	}

	public void updateStat() {

		try {

			String s = String.format("alive=%s %s", m_server.isAlive(), m_server.toString());

			lbStat.setText("<html>" + s);

		} catch (Exception e) {
			lbStat.setText("<html>" + e.toString());
		}

	}
	
	
	
	
	

	public void putOnTray() {
		// Check the SystemTray support
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}
		final PopupMenu popup = new PopupMenu();
		final TrayIcon trayIcon = new TrayIcon(XcMain.createImage("cloud-data.png", "tray icon"));
		final SystemTray tray = SystemTray.getSystemTray();

		// popup.add(serialItem);

		// MenuItem roomStateItem = new MenuItem("room state");
		// popup.add(roomStateItem);

		// MenuItem optionsItem = new MenuItem("options");

		MenuItem exitItem = new MenuItem("exit");
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);
		trayIcon.setToolTip("xcopy server");

		//Log.l("addtray");

		try {

		 
				tray.add(trayIcon);

		} catch (AWTException e) {

			JOptionPane.showMessageDialog(this, "TrayIcon could not be added.");
			System.out.println("TrayIcon could not be added.");
			System.exit(0);
			return;
		}

//		Log.l("after addtray");

		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// JOptionPane.showMessageDialog(null,
				// "This dialog box is run from System Tray");
				frmServer.this.setVisible(true);

			}
		});

		// roomStateItem.addActionListener(new ActionListener() {

		// optionsItem.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// if (use_ext) {
		// DlgLogin d = new DlgLogin("login", gDb.read_cfg("adminpwd"), _this);
		// d.setModal(true);
		// d.setVisible(true);
		//
		// if (d.m_ret == 1) {
		//
		// //dlgOptions.setVisible(true);
		// }
		// }
		// }
		// });

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				// System.exit(0);
				// Show the dialog
				int response = JOptionPane.showConfirmDialog(frmServer.this, // owner
						"exit?", // message
						"confirm", // title
						JOptionPane.YES_NO_OPTION); // optionType

				if (response == JOptionPane.YES_OPTION) {

					// gDb.doShutdown();

					System.exit(0);

					tray.remove(trayIcon);
				}
			}
		});

	}

}
