package jogl3;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;

import javax.swing.JFrame;

import com.jogamp.opengl.awt.GLCanvas;

import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import jogl3.Advanced.RTTManager;
import jogl3.Advanced.Resizer;

public class Controller {
	public float LSlocation = 0;
	public Vec3 lightDirection;
	public float degree = 0;
	public Point e = new Point(0,0);
	public float multiplier = 0.005f;
	public Vec3 scale = new Vec3(1,1,1);
	public Vec3 trans = new Vec3(0,0,0);
	public Point Mstart;
	public Point Mdestination;
	public GLCanvas c;
	public Robot r;
	public JFrame f;
	public RenderControl NDC_swap; // NDC is for Next Draw Command
	public RenderControl NDC_export;// NDC is for Next Draw Command
	public RenderControl NDC_shader;// NDC is for Next Draw Command
	public RTTManager rtm;
	public float yaw = 0;
	public float pitch = 0;
	public int width;
	public int height;
	public Vec4 boundary;
	public int innerbound = 40;
	public Resizer S_canvas;
	public Resizer S_map;
	public Controller(GLCanvas canvas, Robot r,JFrame f) throws AWTException {
		this.S_canvas = new Resizer(800);
		this.S_map = new Resizer(1500);
		this.width = S_canvas.getSize()[0];
		this.height = S_canvas.getSize()[1];
		this.boundary = new Vec4(0,0,this.width,this.height);
		S_canvas.EnableglResize();//If you turn this off . Okay , you got the screen that is able to respond to resizing . But every time you change the viewport to fit render target's size , the following call which draws to the default fbo uses the last setting of RT . It is set upon context , not frame. 
		S_map.EnableglResize();
		this.c = canvas;
		this.r = r;
		this.f = f;
		NDC_swap = new RenderControl("SWAP");
		NDC_export = new RenderControl("EXPORT");
		NDC_shader = new RenderControl("UV MODE");
	}
	public void resetMouse() {
		if(e.y < boundary.getY()) {
			e.y = boundary.getY().intValue()+2;
		}
		if(e.y > boundary.getW()) {
			e.y = boundary.getW().intValue()-2;
		}
		if(e.x < boundary.getX()) {
			e.x = boundary.getX().intValue()+2;
		}
		if(e.x > boundary.getZ()) {
			e.x = boundary.getZ().intValue()-2;
		}
		r.mouseMove(e.x, e.y);
	}
	public void processMouse() {
		float xMove = this.Mdestination.x - this.Mstart.x;
		float yMove = this.Mdestination.y - this.Mstart.y;
		xMove *= this.multiplier;
		yMove *= -this.multiplier;
		this.yaw += xMove;
		this.pitch += yMove;
		pitch = Math.min(pitch, 1);
		pitch = Math.max(pitch, -1);
		this.c.display();
	}
	public void CalculateNewLightLocation() {
		this.lightDirection = new Vec3(Math.sin(LSlocation) , 0 , Math.cos(LSlocation));
		//System.out.println(lightDirection + "----------------------------");
	}
	
	public class RenderControl {
		private boolean flag = false;
		private String self = "";
		public RenderControl(String name) {this.self = name;}
		public boolean IsOn() {return flag;}
		public boolean IsOff() {return !flag;}
		public void TurnOn() {if(this.IsOff()) {flag = true;print("Is On");}}
		public void TurnOff(){if(this.IsOn()) {flag = false;print("Is Off\n");}}
		public void Switch() {flag = !flag;}
		private void print(String name) {System.out.print(" " + self + " " + name);}
	}
}
