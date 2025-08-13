package home.lib.log;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.net.DatagramPacket;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.xcopy.XcMain;
import org.xcopy.frmServer;

 import home.lib.util.TimeUtil;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Dimension;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class FrmDbg extends JFrame {

	private JPanel contentPane;
	private JPanel panel;
	private JScrollPane scrollPane;
	private JTextPane textPane;
	private JLabel lblPort;
	private JTextField txtPort;
	private JLabel lblAnd;
	private JTextField txtFilter;
	private JButton btnUpdate;

	/**
	 * Create the frame.
	 */
	public FrmDbg() {

		putOnTray();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

				if (SystemTray.isSupported()) {

					setVisible(false);

				} else {
					int response = JOptionPane.showConfirmDialog(FrmDbg.this, // owner
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

		setTitle("udp debugger v0.1");
		// setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 585, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		panel = new JPanel();
		panel.setPreferredSize(new Dimension(10, 100));
		contentPane.add(panel, BorderLayout.NORTH);

		lblIp = new JLabel("ip");
		panel.add(lblIp);

		txtIp = new JTextField();
		txtIp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {

			}
		});
		txtIp.setText("114.200.254.181");
		txtIp.setColumns(16);
		panel.add(txtIp);

		lblPort = new JLabel("port");
		panel.add(lblPort);

		txtPort = new JTextField();
		txtPort.setText("10150");
		panel.add(txtPort);
		txtPort.setColumns(6);

		lblAnd = new JLabel("filter");
		panel.add(lblAnd);

		txtFilter = new JTextField();
		txtFilter.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {

				System.out.println(txtFilter.getText());

				if (m_receiver != null) {
					m_receiver.setFilters(txtFilter.getText());
				}

			}
		});
		panel.add(txtFilter);
		txtFilter.setColumns(30);

		btnUpdate = new JButton("update(udp)");
		btnUpdate.addActionListener(new BtnUpdateActionListener());
		panel.add(btnUpdate);

		btnUpdatetcp = new JButton("update(tcp)");
		btnUpdatetcp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				init(txtIp.getText(), Integer.valueOf(txtPort.getText().trim()), txtFilter.getText());

				append("start dbg \n");

			}
		});
		panel.add(btnUpdatetcp);

		chkAutoScroll = new JCheckBox("auto scroll");
		panel.add(chkAutoScroll);

		btnNewButton = new JButton("clear");
		btnNewButton.addActionListener(new BtnNewButtonActionListener());

		chkStopLog = new JCheckBox("stop log");
		panel.add(chkStopLog);
		panel.add(btnNewButton);

		scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		textPane = new JTextPane();
		scrollPane.setViewportView(textPane);

	}

	private static Object sync = new Object();

	MyLoggerReceiver m_receiver = null;
	private JButton btnNewButton;
	private JCheckBox chkAutoScroll;
	private JCheckBox chkStopLog;
	private JTextField txtIp;
	private JLabel lblIp;
	private JButton btnUpdatetcp;

	public void init(String ip, int dport, String search_word) {

		synchronized (sync) {

			append(String.format("start udp svr port=%s search(%s)  \n", dport, search_word));

			try {
				m_receiver.close();
			} catch (Exception e) {

			}

			try {

				m_receiver = new MyLoggerReceiver();
				m_receiver.setEncode("utf-8").addListener(new MyLoggerListener() {

					@Override
					public void ulogReceive(Object dp, String when, String from, int level, String str) {

						for (String a : str.split("[\\r\\n]+")) {

							String s = String.format("l%d [%s][%s] %s \r\n", level, from, when, a.trim());
							append(s);

						}

					}

					@Override
					public void ulogClosed(Object o) {

						append("ulog closed");

					}
				});

				m_receiver.start(ip, dport, search_word);// .get();

				TimeUtil.sleep(1000);

				append("started %s \n", m_receiver.isAlive());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				append(e.toString());
			}
		} // synchronized(sync) {

	}

	private class BtnUpdateActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			init(null, Integer.valueOf(txtPort.getText().trim()), txtFilter.getText());

			append("start dbg \n");
		}
	}

	private class BtnNewButtonActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			textPane.setText("");
		}
	}

	public void append(String f, Object... a) {
		append(String.format(f, a));

	}

	public void append(String s) {

		if (chkStopLog.isSelected())
			return;

		StyledDocument doc = textPane.getStyledDocument();

		// Define a keyword attribute

		SimpleAttributeSet keyWord = new SimpleAttributeSet();
		StyleConstants.setForeground(keyWord, Color.RED);
		StyleConstants.setBackground(keyWord, Color.YELLOW);
		StyleConstants.setBold(keyWord, true);

		// Add some text

		try {
			doc.insertString(doc.getLength(), s, null);
			// doc.insertString(0, s, null);
			// doc.insertString(doc.getLength(), "\nEnd of text", keyWord );
		} catch (Exception e) {
			System.out.println(e);
		}

		if (doc.getLength() > 1024 * 128) {
			try {
				doc.remove(0, 1024);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// textPane.setCaretPosition(textPane.getText().length());

		if (chkAutoScroll.isSelected()) {
			textPane.setCaretPosition(doc.getLength());
			DefaultCaret caret = (DefaultCaret) textPane.getCaret();
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		} else {
			DefaultCaret caret = (DefaultCaret) textPane.getCaret();
			caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

		}

	}

	public void putOnTray() {
		// Check the SystemTray support
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}
		final PopupMenu popup = new PopupMenu();
		final TrayIcon trayIcon = new TrayIcon(createImage("log-format.png", "tray icon"));
		final SystemTray tray = SystemTray.getSystemTray();

		// popup.add(serialItem);

		// MenuItem roomStateItem = new MenuItem("room state");
		// popup.add(roomStateItem);

		// MenuItem optionsItem = new MenuItem("options");

		MenuItem exitItem = new MenuItem("exit");
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);
		trayIcon.setToolTip("logging");

		// Log.l("addtray");

		try {

			tray.add(trayIcon);

		} catch (AWTException e) {

			JOptionPane.showMessageDialog(this, "TrayIcon could not be added.");
			System.out.println("TrayIcon could not be added.");
			System.exit(0);
			return;
		}

		// Log.l("after addtray");

		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// JOptionPane.showMessageDialog(null,
				// "This dialog box is run from System Tray");
				FrmDbg.this.setVisible(true);

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
				int response = JOptionPane.showConfirmDialog(FrmDbg.this, // owner
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

	/**
	 * 
	 * @param path
	 * @param description
	 * @return
	 */
	// Obtain the image URL
	public static Image createImage(String path, String description) {
		URL imageURL = FrmDbg.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage().getScaledInstance(16, 16, 0);
		}
	}

}
