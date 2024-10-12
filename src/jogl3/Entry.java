package jogl3;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Robot;

import javax.swing.JButton;
import javax.swing.JFrame;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import jogl3.Manipulation.KeyListener;
import jogl3.Manipulation.MouseClick;
import jogl3.Manipulation.MouseMovement;
import jogl3.Trace.TraceListener;

public class Entry {

	public static void main(String[] args) throws AWTException {
		GLCapabilities CanvasParameters = new GLCapabilities(GLProfile.getDefault());
		CanvasParameters.setDoubleBuffered(false);//Needs more configuration //This causes FBO flashing issue while swapping
		GLCanvas canvas = new GLCanvas(CanvasParameters);
		JFrame frame = new JFrame("毕业设计");
		Robot r = new Robot();
		Controller c = new Controller(canvas,r,frame);
		MouseMovement mm = new MouseMovement(c);
		MouseClick mc = new MouseClick(c);
		canvas.addKeyListener(new KeyListener(c));
		//canvas.addMouseMotionListener(mm);
		canvas.addMouseListener(mc);
		canvas.addGLEventListener(new EventListener(c));//Event Listener is an interface we can use to manipulate the GLDrawable
		frame.setSize(new Dimension(c.width,c.height));
		frame.getContentPane().add(canvas,BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		
		
		FPSAnimator anm = new FPSAnimator(1);
		JFrame sidepanel = new JFrame("光追");
		sidepanel.setSize(550, 500);
		JButton b = new JButton("1");
		JButton a = new JButton("2");
		sidepanel.getContentPane().add(b,BorderLayout.EAST);
		sidepanel.getContentPane().add(a,BorderLayout.WEST);
		GLCanvas trace = new GLCanvas();
		Robot r2 = new Robot();
		Controller c2 = new Controller(trace,r2,sidepanel);
		MouseClick mc2 = new MouseClick(c2);
		trace.addMouseListener(mc2);
		trace.addGLEventListener(new TraceListener(c2));
		sidepanel.getContentPane().add(trace,BorderLayout.CENTER);
		//anm.add(trace);
		//anm.start();
		//sidepanel.setVisible(true);
	}

}
