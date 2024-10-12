package jogl3.Trace;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import jogl3.Controller;

public class TraceListener  implements GLEventListener{

	RayTraceProgram rtp= new RayTraceProgram();
	public Controller c;
	
	public TraceListener(Controller c) {
		this.c = c;
	}
	
	@Override
	public void display(GLAutoDrawable arg0) {
		GL3 gl = arg0.getGL().getGL3();
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL3.GL_DEPTH_BUFFER_BIT); //Without this there's depth bug
		if(rtp.Drawable()) {
			rtp.BindContext(arg0);
			rtp.Display("QUAD",c);
			rtp.UnbindContext();
			System.out.print("渲染");
		}
		rtp.Drawable(true);
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GLAutoDrawable arg0) {
		GL3 gl = arg0.getGL().getGL3();
        gl.glDepthFunc(GL3.GL_LESS);
        gl.glEnable(GL3.GL_DEPTH_TEST);
		rtp.BindContext(arg0);
		rtp.CreateGeo("QUAD");
		rtp.CreateGeo("TRACE_TARGET_IMPORT");
		rtp.InstallShader();
		rtp.Drawable(false);
		rtp.UnbindContext();
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		
	}

}
