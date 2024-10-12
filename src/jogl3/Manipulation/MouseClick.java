package jogl3.Manipulation;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import jogl3.Controller;

public class MouseClick extends MouseAdapter{
	public Controller c;
	public MouseClick(Controller c) {
		this.c = c;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		c.Mstart = e.getLocationOnScreen();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		c.Mdestination = e.getLocationOnScreen();
		c.processMouse();
	}
}
