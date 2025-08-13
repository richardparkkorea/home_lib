package home.lib.swing;

import javax.swing.JLabel;

import java.awt.*;
import java.util.ArrayList;

@Deprecated
public class FlowLayout2 extends FlowLayout {

	//
	ArrayList<Integer> m_rowLoc = new ArrayList<Integer>();
	//
	ArrayList<Integer> m_newLineLoc = new ArrayList<Integer>();

	boolean m_useFixWidth = false;
	int m_fixWidth = 100;

	boolean m_autoHeight = true;

	/**
	 * 
	 */
	private static final long serialVersionUID = 4242230330300838771L;
	public int componentMaxY = 0;

	public FlowLayout2() {
		this(CENTER, 5, 5, false);
	}

	public FlowLayout2(int align) {
		this(align, 5, 5, false);
	}

	public FlowLayout2(int align, int hgap, int vgap) {
		this( align,hgap,vgap,false);
 	}
	
	public FlowLayout2(int align, int hgap, int vgap, boolean ah) {
		super.setHgap(hgap);
		super.setVgap(vgap);
		setAlignment(align);
		m_autoHeight = ah;
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
			boolean ltr, boolean useBaseline, int[] ascent, int[] descent) {

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
				x += m.getWidth() + getHgap();
			}
		}
		return height;
	}

	@Override
	public void layoutContainer(Container target) {

		synchronized (m_rowLoc) {
			m_rowLoc.clear();
			m_rowLoc.add(new Integer(0));
			m_newLineLoc.clear();
			m_newLineLoc.add(new Integer(0));

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

				for (int i = 0; i < nmembers; i++) {
					Component m = target.getComponent(i);
					if (m.isVisible()) {
						Dimension d = m.getPreferredSize();
						m.setSize(d.width, d.height);

						if ((x == 0) || ((x + d.width) <= maxwidth) && newline == false) {
							if (x > 0) {
								x += getHgap();
							}
							x += d.width;
							rowh = Math.max(rowh, d.height);
						} else {
							rowh = moveComponents(target, insets.left + getHgap(), y, maxwidth - x, rowh, start, i, ltr,
									useBaseline, ascent, descent);
							x = d.width;
							y += getVgap() + rowh;
							rowh = d.height;
							start = i;

							if (newline == true) {
								newline = false;
								m_newLineLoc.add(y);
							}
							m_rowLoc.add(y);
						}

						if (m instanceof JLabel) {
							if (((JLabel) m).getText().equals("<html><br>")) {
								newline = true;
								((JLabel) m).setOpaque(false);
							}

						}
					}
				}
				int rh = moveComponents(target, insets.left + getHgap(), y, maxwidth - x, rowh, start, nmembers, ltr,
						useBaseline, ascent, descent);

				// System.out.println(" " + maxwidth + " " + y);
				componentMaxY = y + rh;
				m_rowLoc.add(componentMaxY);
				// if (newline == true) {
				m_newLineLoc.add(componentMaxY);
				// }

				Dimension dm = target.getPreferredSize();
				Dimension sm = target.getSize();

				// if the drawing area of ctrl is bigger than panel's height then expend it.

				if (m_autoHeight) {
					if ((dm.height != componentMaxY || dm.width != sm.width) && componentMaxY > 10 && target != null) {

						dm.width = sm.width;
						// System.out.format(" height=%d , w=%d (%s) (%s) \r\n", dm.height,dm.width , target.isValid(),
						// target.getParent());

						dm.height = componentMaxY;
						target.setPreferredSize(dm);
						target.setSize(dm);
						if (target.isValid()) {
							target.validate();
							target.repaint();
							if (target.getParent() != null) {
								target.getParent().validate();
								target.getParent().repaint();
							}
						}
					}
				}

				// dm=target.getSize();
				// dm.height= componentMaxY;
				// target.setSize( dm );

			} // sync

		} // sync
	}

	/**
	 * 
	 * 
	 * 
	 * 
	 */
	public Integer[] getHeights() {
		synchronized (m_rowLoc) {
			return m_rowLoc.toArray(new Integer[0]);
		}
	}

	public Integer[] getNewLineHeights() {
		synchronized (m_rowLoc) {
			return m_newLineLoc.toArray(new Integer[0]);
		}
	}
}
