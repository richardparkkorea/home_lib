package home.lib.swing;

import javax.swing.JComponent;
import javax.swing.JLabel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FlowLayout3 extends FlowLayout {

	// //
	// ArrayList<Integer> m_rowLoc = new ArrayList<Integer>();
	// ArrayList<Integer> m_colLoc = new ArrayList<Integer>();
	// //
	// ArrayList<Integer> m_newLineLoc = new ArrayList<Integer>();

	boolean m_useFixWidth = false;
	// int m_fixWidth = 100;
	boolean m_autoHeight = false;

	/**
	 * 
	 */
	private static final long serialVersionUID = 4242230330300838771L;
	int componentMaxY = 0;

	public FlowLayout3() {
		super(CENTER, 5, 5);
	}

	public FlowLayout3(int align) {
		super(align, 5, 5);
	}

	public FlowLayout3(int align, int hgap, int vgap) {
		this(align, hgap, vgap, false);
	}

	public FlowLayout3(int align, int hgap, int vgap, boolean autoHeight) {
		super.setHgap(hgap);
		super.setVgap(vgap);
		setAlignment(align);
		m_autoHeight = autoHeight;

	}

	@Override
	public Dimension preferredLayoutSize(Container target) {
		synchronized (target.getTreeLock()) {
			return new Dimension(10, componentMaxY);
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container target) {
		synchronized (target.getTreeLock()) {

			return new Dimension(10, componentMaxY);

		}
	}

	private int moveComponents(Container target, int x, int y, int width, int height, int rowStart, int rowEnd,
			boolean ltr, boolean useBaseline, int[] ascent, int[] descent, int xloc[]) {

		switch (getAlignment()) {
		case LEFT:
			x += ltr ? 0 : width;
			break;
		case CENTER:
			x += width / 2;
			break;
		case RIGHT:
			x += ltr ? width : 0;
			break;
		case LEADING:
			break;
		case TRAILING:
			x += width;
			break;
		}

		// System.out.println(" ==> "+useBaseline +" "+ltr);

		int maxAscent = 0;
		int nonbaselineHeight = 0;
		int baselineOffset = 0;
		int p = 0;

		for (int i = rowStart; i < rowEnd; i++) {
			Component m = target.getComponent(i);
			if (m.isVisible()) {
				int cy;

				if (useBaseline && ascent[i] >= 0) {
					cy = y + baselineOffset + maxAscent - ascent[i];
				} else {
					cy = y;// + (height - m.getHeight()) / 2;
				}

				if (ltr) {
					m.setLocation(x, cy);
				} else {
					m.setLocation(target.getWidth() - x - m.getWidth(), cy);
				}

				if (xloc == null)
					x += m.getWidth() + getHgap();
				else
					x += xloc[p++] + getHgap();
			}
		}
		return height;
	}

	@Override
	public void layoutContainer(Container target) {

		// synchronized (m_rowLoc) {

		//
		// m_rowLoc.clear();
		// m_rowLoc.add(new Integer(0));
		//
		// m_colLoc.clear();
		// m_colLoc.add(new Integer(0));
		//
		//
		// m_newLineLoc.clear();
		// m_newLineLoc.add(new Integer(0));

		synchronized (target.getTreeLock()) {
			Insets insets = target.getInsets();
			int maxwidth = target.getWidth() - (insets.left + insets.right + getHgap() * 2);
			int nmembers = target.getComponentCount();
			int x = 0, y = insets.top + getVgap();
			int rowh = 0, start = 0;

			boolean newline = false;
			// System.out.println(" maxwidth = " + maxwidth);

			boolean ltr = target.getComponentOrientation().isLeftToRight();

			boolean useBaseline = getAlignOnBaseline();
			int[] ascent = null;
			int[] descent = null;

			if (useBaseline) {
				ascent = new int[nmembers];
				descent = new int[nmembers];
			}

			// int after=0;
			int xloc[] = null;
			int xp = 0;
			boolean shouldBeNull = false;
			// boolean check = false;

			int count = 0;

			for (int i = 0; i < nmembers; i++) {
				Component m = target.getComponent(i);
				count++;
				if (m.isVisible()) {

					Dimension d = m.getPreferredSize();
					m.setSize(d.width, d.height);

					// after++;

					if ((x == 0) || ((x + d.width) <= maxwidth) && newline == false) {
						if (x > 0) {
							x += getHgap();
						}

						if (xloc != null)
							x += xloc[xp++];
						else
							x += d.width;

						// System.out.println( " "+xp+" "+xloc[xp]);

						rowh = Math.max(rowh, d.height);

					} else {
						xp = 0;

						rowh = moveComponents(target, insets.left + getHgap(), y, maxwidth - x, rowh, start, i, ltr,
								useBaseline, ascent, descent, xloc);

						if (xloc != null)
							x = xloc[xp++];
						else
							x = d.width;

						// System.out.println( " "+xp+" "+xloc[xp]);

						y += getVgap() + rowh;
						rowh = d.height;
						start = i;

						if (newline == true) {
							newline = false;
							// m_newLineLoc.add(y);

						}

						// if(shouldBeNull) {
						// shouldBeNull=false;
						// xloc=null;
						// }

					}

					if (m instanceof JLabel) {
						if (((JLabel) m).getText().equals("<html><br>")) {
							newline = true;
							((JLabel) m).setOpaque(false);
							// ((JLabel) m).setOpaque(false);
						}
						if (((JLabel) m).getText().equals("<html><start>")) {
							// newline = true;
							// ((JLabel) m).setOpaque(false);
							// check = true;
							xloc = getXGrid(target, i);
							((JLabel) m).setOpaque(false);
						}
						if (((JLabel) m).getText().equals("<html><stop>")) {
							// newline = true;
							// ((JLabel) m).setOpaque(false);
							// check = false;
							// after=0;
							xloc = null;
							((JLabel) m).setOpaque(false);
							// shouldBeNull=true;
						}

					}
				}
			}

			// int xw[]=null;
			// if( check)
			// xw=xloc;

			int rh = moveComponents(target, insets.left + getHgap(), y, maxwidth - x, rowh, start, nmembers, ltr,
					useBaseline, ascent, descent, xloc);

			// System.out.println(" " + maxwidth + " " + y);
			componentMaxY = y + rh;
			// m_rowLoc.add(componentMaxY);
			// if (newline == true) {
			// m_newLineLoc.add(componentMaxY);
			// }

			Dimension pm = target.getPreferredSize();
			Dimension dm = target.getSize();// target.getPreferredSize();

			// System.out.println("height="+componentMaxY );
			// if the drawing area of ctrl is bigger than panel's height then expend it.

			if (count > 0) {
				if ((dm.height != componentMaxY || pm.width != dm.width) && m_autoHeight && componentMaxY > 10
						&& target != null) {
					dm.height = componentMaxY;

					target.setPreferredSize(dm);
					target.setSize(dm);
					// target.validate();
					// target.repaint();
					// target.getParent().validate();
					// target.getParent().repaint();
				} else if (count == 0) {
					dm.height = 1;
					target.setPreferredSize(dm);
					target.setSize(dm);
				}
			}

			// dm=target.getSize();
			// dm.height= componentMaxY;
			// target.setSize( dm );

		} // sync

		// } // sync
	}

	public int[] getXGrid(Container target, int istart) {

		// synchronized (m_rowLoc) {

		//
		// m_rowLoc.clear();
		// m_rowLoc.add(new Integer(0));
		//
		// m_colLoc.clear();
		// m_colLoc.add(new Integer(0));
		//
		//
		// m_newLineLoc.clear();
		// m_newLineLoc.add(new Integer(0));

		// ArrayList<Integer> m_xp=new ArrayList<Integer>();
		int p = 0;
		int xloc[] = new int[256];
		boolean check = false;

		int maxN = 0;

		synchronized (target.getTreeLock()) {
			Insets insets = target.getInsets();
			int maxwidth = target.getWidth() - (insets.left + insets.right + getHgap() * 2);
			int nmembers = target.getComponentCount();
			int x = 0, y = insets.top + getVgap();
			int rowh = 0, start = 0;

			boolean newline = false;

			for (int i = istart; i < nmembers; i++) {
				Component m = target.getComponent(i);
				if (m.isVisible()) {
					Dimension d = m.getPreferredSize();
					// m.setSize(d.width, d.height);

					if ((x == 0) || ((x + d.width) <= maxwidth) && newline == false) {
						if (x > 0) {
							x += getHgap();
						}
						x += d.width;
						rowh = Math.max(rowh, d.height);

						if (check)
							xloc[p] = Math.max(xloc[p], d.width);
						p++;

						maxN = Math.max(p, maxN);

					} else {

						x = d.width;
						y += getVgap() + rowh;
						rowh = d.height;
						start = i;

						if (newline == true) {
							newline = false;
							// m_newLineLoc.add(y);
						}
						// m_rowLoc.add(y);

						p = 0;
						if (check)
							xloc[p] = Math.max(xloc[p], d.width);
						p++;
					}

					if (m instanceof JLabel) {
						if (((JLabel) m).getText().equals("<html><br>")) {
							newline = true;
							// ((JLabel) m).setOpaque(false);
						}

						if (((JLabel) m).getText().trim().equals("<html><stop>")) {
							check = false;
							// newline = true;
							// ((JLabel) m).setOpaque(false);
							// System.out.println("check-----2");
							// for (int n = 0; n < maxN; n++) {
							// System.out.println("n" + n + "=" + xloc[n]);
							// }

							// } // sync

							return Arrays.copyOf(xloc, maxN + 10);

						}

						if (((JLabel) m).getText().trim().equals("<html><start>")) {
							check = true;
							// newline = true;
							// ((JLabel) m).setOpaque(false);
							// System.out.println("check-----1");
						}

					}
				}
			}

		} // sync

		// for (int i = 0; i < maxN; i++) {
		// System.out.println("n" + i + "=" + xloc[i]);
		// }

		// } // sync

		return Arrays.copyOf(xloc, maxN + 10);
	}

	// /**
	// *
	// *
	// *
	// *
	// */
	// public Integer[] getHeights() {
	// synchronized (m_rowLoc) {
	// return m_rowLoc.toArray(new Integer[0]);
	// }
	// }
	//
	// public Integer[] getNewLineHeights() {
	// synchronized (m_rowLoc) {
	// return m_newLineLoc.toArray(new Integer[0]);
	// }
	// }
}
