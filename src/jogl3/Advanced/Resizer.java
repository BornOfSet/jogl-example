package jogl3.Advanced;

import java.awt.Component;

import com.jogamp.opengl.GLAutoDrawable;


enum _ini{
	square,width_height;
}

public class Resizer {
	private Standard size;
	private boolean GLRESIZE = false;
	private _ini iniMethod;
	public Resizer(int square) {
		if(size==null) {
		size = new Standard();
		size.width = square;
		size.height = square;}
		else {System.out.println("\nERROR : Resizer has been initialized . Create another Resizer!");}
		iniMethod = _ini.square;
	}
	public Resizer(int width, int height) {
		if(size==null) {
		size = new Standard();
		size.width = width;
		size.height = height;}
		else {System.out.println("\nERROR : Resizer has been initialized . Create another Resizer!");}
		iniMethod = _ini.width_height;
	}
	public void setsize(Component c) {
		c.setSize(size.width, size.height);
	}
	public void EnableglResize() {
		GLRESIZE = true;
	}
	public void DisableglResize() {
		GLRESIZE = false;
	}	
	public void glResize(GLAutoDrawable Drawable) {
		//Resize the DC(device coordinate / screen) anyway . 
		//Not only resizing it for event listener reshape callback
		//But also record the screen coordinate produced by last reshaping action
		//And apply the same coordinate for RTM
		//Otherwise you have to pass that fucking c,d value from GLEL to RTM that's sheeeeit
		if(!GLRESIZE) {return;}
		int c = size.width;
		int d = size.height;
		int square = Math.min(c, d);
		int xoffset = (c-square)/2;
		int yoffset = (d-square)/2;
		Drawable.getGL().getGL3().glViewport(xoffset, yoffset, square, square);
	}
	public void glResize(GLAutoDrawable Drawable, int c,int d) {
		size.width = c;
		size.height = d;
		glResize(Drawable);
	}
	public int[] getSize() {
		int[] x = {this.size.width, this.size.height};
		return x;
	}
	public int getSquareSize() {
		if(this.iniMethod == _ini.width_height) {System.out.println("\nERROR : Cannot Return Width_Height when Resized is Initialized as Square!"); return -1;	}
		else {
			return size.width;
		}
	}
	
	private class Standard{
		public int width = 0;
		public int height = 0;
	}
}
