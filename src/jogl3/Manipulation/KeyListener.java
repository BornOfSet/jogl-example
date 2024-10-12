package jogl3.Manipulation;

import glm_.vec3.Vec3;
import jogl3.Controller;
import jogl3.headers.iKeyListener;

public class KeyListener extends iKeyListener{

	public Controller c;
	
	public KeyListener(Controller c) {
		this.c = c;
	}
	
	
	@Override
	public void OnPressedW() {
		//c.trans = c.trans.plus(new Vec3(0,0,0.1));
		c.scale = c.scale.times(1.25);
		c.c.display();
	}

	@Override
	public void OnPressedS() {
		//c.trans = c.trans.plus(new Vec3(0,0,-0.1));
		c.scale = c.scale.times(0.8);
		c.c.display();
	}

	@Override
	public void OnPressedA() {
		c.trans = c.trans.plus(new Vec3(-0.1,0,0));
		c.c.display();
	}

	@Override
	public void OnPressedD() {
		c.trans = c.trans.plus(new Vec3(0.1,0,0));
		c.c.display();
	}

	@Override
	public void OnPressedQ() {
		c.trans = c.trans.plus(new Vec3(0,-0.1,0));
		c.c.display();
	}

	@Override
	public void OnPressedE() {
		c.trans = c.trans.plus(new Vec3(0,0.1,0));
		c.c.display();
	}
	

	//Here we should be swapping the FBO to render in
	@Override
	public void OnPressedSpace() { //Render into attachment0 or default ?
		c.NDC_swap.TurnOn();
	}


	@Override
	public void OnShiftSpace() { //Screenshot the attachment0 buffer
		c.NDC_export.TurnOn();
	}


	@Override
	public void OnShiftA() {
		c.LSlocation += 0.1;
		c.c.display();
	}


	@Override
	public void OnShiftD() {
		c.LSlocation -= 0.1;
		c.c.display();
	}


	//change shader
	@Override
	public void OnPressedV() {
		c.NDC_shader.Switch();
	}

}
