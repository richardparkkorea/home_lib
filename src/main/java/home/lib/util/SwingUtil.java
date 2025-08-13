
package home.lib.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.TimerTask;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import home.lib.io.FileProperties;
import home.lib.lang.Timer2;

public class SwingUtil {

	public static Graphics m_graph = null;

	public static Dimension getStringSize(Font f, String s) {

		if (m_graph == null)
			m_graph = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB).getGraphics();

		FontMetrics metrics = m_graph.getFontMetrics(f);
		return new Dimension(metrics.stringWidth(s), metrics.getHeight());

	}

	/**
	 * 
	 * 
	 * 
	 * @param p
	 */
	public static void setCenterLocation(Component p) {
		// Component p=dlg;
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		int x = (screenSize.width - p.getWidth()) / 2;
		int y = (screenSize.height - p.getHeight()) / 2;
		p.setLocation(x, y);
	}

	public static void setCenterLocation(Component p, Component parent) {
		// Component p=dlg;
		// final Toolkit toolkit = Toolkit.getDefaultToolkit();
		// final Dimension screenSize = toolkit.getScreenSize();

		Point l = parent.getLocation();

		Dimension s = parent.getSize();

		int x = l.x + (s.width - p.getWidth()) / 2;
		int y = l.y + (s.height - p.getHeight()) / 2;
		p.setLocation(x, y);
	}

	/**
	 * 
	 * @param parent
	 * @param bk
	 * @param fo
	 * @return
	 */
	public static int changeBackground(Container parent, Color bk, Color fo) {

		Component[] com = parent.getComponents();

		if (com == null || com.length == 0)
			return 0;

		int cnt = 0;

		for (Component c : com) {

			c.setBackground(bk);
			c.setForeground(fo);

			if (c instanceof Container) {
				cnt += changeBackground((Container) c, bk, fo);
			}
		}

		return cnt;

	}

	/**
	 * open file dialog<br>
	 * 
	 * @param parent
	 *            -
	 * @param filter
	 * @param openORsave
	 * @param fp
	 * @param fpname
	 * @return
	 */
	public static File chooseFile(Component parent, FileFilter filter, boolean openORsave, FileProperties fp,
			String fpname) {
		//
		JFileChooser m_fc = new JFileChooser();

		try {
			File workingDirectory = new File(fp.getString(fpname));
			m_fc.setCurrentDirectory(workingDirectory);
		} catch (Exception e2) {
			;
		}

		if (filter != null) {
			m_fc.resetChoosableFileFilters();
			m_fc.setAcceptAllFileFilterUsed(false);
			m_fc.addChoosableFileFilter(filter);
		}

		int returnVal = 0;
		if (openORsave)
			returnVal = m_fc.showOpenDialog(parent);
		else
			returnVal = m_fc.showSaveDialog(parent);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = m_fc.getSelectedFile();

			fp.setString(fpname, file.getAbsolutePath());
			// gDb.write_cfg(name + "path", );

			return file;
		}

		return null;
	}

	/**
	 * 
	 * 
	 * @param pl
	 * @param t
	 */
	public static void autoFitTableColumnWidth(JPanel pl, JTable t) {

		autoFitTableColumnWidth(pl, t, null, true);

	}

	/**
	 * 
	 * @param pl
	 * @param t
	 * @param ext
	 * @param checkColumn
	 *            - also check the width of column text
	 */
	public static void autoFitTableColumnWidth(final JPanel pl, final JTable t, final String[] ext,
			final boolean checkColumn) {

		// sometime the javaUI hangs on due to adjust of column size
		// that's why i use the Timer
		new Timer2().schedule(new TimerTask() {
			public void run() {

				// System.out.println("adjust table column");

				if (pl.getGraphics() == null)
					return;

				if (t == null)
					return;

				TableModel tm = t.getModel();

				if (t == null || tm == null) {
					System.out.println("autoFitTableColumnWidth err-1");
					return;

				}

				int scw = 0;// sum of columns width

				// System.out.println("autoFitTableColumnWidth---0");
				t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

				TableColumnModel columnModel = t.getColumnModel();
				for (int col = 0; col < t.getColumnCount(); col++) {

					int width = 10; // Min width

					// column
					Font font = t.getFont();
					FontMetrics metrics = pl.getGraphics().getFontMetrics(font);
					int hgt = metrics.getHeight();
					int adv = 0;
					if (checkColumn)
						adv = metrics.stringWidth(tm.getColumnName(col));

					width = adv + 20;

					if (ext != null && col < ext.length) {
						int w = metrics.stringWidth(ext[col]);
						width = Math.max(width, w + 10);

					}

					for (int row = 0; row < t.getRowCount(); row++) {

						String s = "" + t.getValueAt(row, col);
						int w = metrics.stringWidth(s);
						width = Math.max(width, w + 10);

					}

					columnModel.getColumn(col).setPreferredWidth(width);

					scw += width;
				}

				int tw = t.getParent().getSize().width;// scroll width

				// System.out.format(" table w=%d scw=%d ", tw,scw );
				if (scw < tw) {
					double div = (double) tw / scw;

					for (int col = 0; col < t.getColumnCount(); col++) {
						double w = columnModel.getColumn(col).getPreferredWidth();
						columnModel.getColumn(col).setPreferredWidth((int) (w * div));
					}

				}

			}
		}, 100);

	}

	/**
	 * v 1.806
	 * 
	 * @param t
	 * @param n
	 */
	public static void hideTabPage(JTabbedPane t, String n) {
		for (int i = 0; i < t.getTabCount(); i++) {

			if (t.getTitleAt(i).equals(n)) {
				t.removeTabAt(i);
				return;
			}

		} // for
	}

	/**
	 * 
	 * 
	 * @param p
	 * @param b
	 */
	public static void setEnable(Container p, boolean b) {
		if (p == null)
			return;

		for (Component c : p.getComponents()) {

			c.setEnabled(b);

			if (c instanceof Container) {
				setEnable((Container) c, b);
			}
		}

	}

	/**
	 * 
	 * @param cbx
	 * @param n
	 * @return
	 */
	public static boolean setSelectedItem(JComboBox cbx, Object n) {

		for (int h = 0; h < cbx.getItemCount(); h++) {

			if (cbx.getItemAt(h).equals(n)) {
				cbx.setSelectedIndex(h);

				if (cbx.getSelectedItem().equals(n)) {
					cbx.repaint();
					return true;
				}

			}
		}
		return false;

	}

	/**
	 * 
	 * @param cbx
	 * @param n
	 * @return
	 */
	public static int findItem(JComboBox cbx, Object n) {

		for (int h = 0; h < cbx.getItemCount(); h++) {

			if (cbx.getItemAt(h).equals(n)) {

				return h;
			}
		}
		return -1;

	}

	/**
	 * Make a color brighten.
	 *
	 * @param color
	 *            Color to make brighten.
	 * @param fraction
	 *            Darkness fraction.
	 * @return Lighter color.
	 */
	public static Color brighten(Color color, double fraction) {

		int red = (int) Math.round(Math.min(255, color.getRed() + 255 * fraction));
		int green = (int) Math.round(Math.min(255, color.getGreen() + 255 * fraction));
		int blue = (int) Math.round(Math.min(255, color.getBlue() + 255 * fraction));

		int alpha = color.getAlpha();

		return new Color(red, green, blue, alpha);

	}
}
