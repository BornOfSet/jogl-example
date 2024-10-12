package jogl3.Manipulation;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import glm_.vec4.Vec4;
import jogl3.Controller;

public class MouseMovement implements MouseMotionListener{
	public Controller c;
	
	public MouseMovement(Controller c) {
		this.c = c;
	}
	
	@Override
	public void mouseDragged(MouseEvent e) { //如果未来要做拖入模型的脚本，那么应该按下鼠标时可以跨越窗口
		Point loc = c.f.getLocationOnScreen();
		c.boundary = new Vec4(loc.x+c.innerbound,loc.y+c.innerbound,loc.x+c.width-c.innerbound,loc.y+c.height-c.innerbound/2);
		c.e = e.getLocationOnScreen();
		c.resetMouse();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Point loc = c.f.getLocationOnScreen();
		c.boundary = new Vec4(loc.x+c.innerbound,loc.y+c.innerbound,loc.x+c.width-c.innerbound,loc.y+c.height-c.innerbound/2);
		c.e = e.getLocationOnScreen();
		c.resetMouse();
	}
}
