package jogl3;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import jogl3.Advanced.Pass0;
import jogl3.Advanced.RTTManager;
import jogl3.Baker.RefMesh;

public class EventListener  implements GLEventListener{
	
	public Model m;
	public Controller c;
	
	public EventListener(Controller c) {
		this.c = c;
	}
	
	@Override
	public void init(GLAutoDrawable Drawable) {
		GL3 gl = Drawable.getGL().getGL3();
		m = new Model();
		m.LoadScene("C:/Users/User/Desktop/StdL.obj");
		System.out.print(m.GetNumMeshes() + "m.GetNumMeshes()");
		for(int i=0;i<m.GetNumMeshes();i++) {
			m.GetMesh(i).SetupContext(Drawable);
		}
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL3.GL_LESS);

        RTTManager rtm = new RTTManager(Drawable , c);
        this.c.rtm = rtm;
        //Pass0.init(Drawable);
        //这一几何体是单一网格，拥有一套独立的顶点索引
       // Model hiddenMesh = new Model();//这一几何体是隐藏的，它没有装配对应的VBO，直接作为TBO输入shader，你只能在shader中看到它
        RefMesh high = new RefMesh();
        high.LoadFromPath("C:/Users/User/Desktop/raytest.obj" , gl);
	}
	@Override
	public void dispose(GLAutoDrawable Drawable) {
		
	}
	@Override
	public void reshape(GLAutoDrawable drawable, int a, int b, int c,int d) {
		this.c.width = c;
		this.c.height = d;
		//Before we can go into display , and afer init is called . We call Reshape . 
		//When an OpenGL context is first attached to a window, width and height are set to the dimensions of that window.
		//drawable.getGL().getGL3().glViewport(0, 0, 200, 200);
		//Record window's reshaping information anyway . In this case AFTER switching to offscreen FBO , if you reshape the screen , then switch back to the default one : You won't get misplacement error on the default screen which is caused by skipping frame reshaping information during the offscreen period
		//BUG DETECTED//
		//When you're switching back from RT to default and rendering ,there would be aliasing in the screen if you have reshaped the black screen.
		this.c.S_canvas.glResize(drawable,c,d);
		if(this.c.rtm.IsRenderingToTarget()) {   this.c.S_map.glResize(drawable);	}
	}
	@Override
	public void display(GLAutoDrawable Drawable) {
		GL3 gl = Drawable.getGL().getGL3();
		//Pass0.display(Drawable);
		//Here we're cleansing the last buffer . So if we're going to swap buffer and render into attachment0 then we're cleansing the default buffer. Because before swap happened , the exised one is default
		//If we don't clean the default buffer ( ie. put these two codes below the if section) We can see stuffs when we're not rendering into default screen
		//***Make sure every time Display function is starting , we will cleanse the last drawcall , instead of being making room for the following one
		//If you're going to switch from attachment0 back to the default screen , you're swapping the buffer AFTER attachment0 is set to blackness
		//Thus if you're going to switch twice , from the default screen back again to attachment0 , the screen should remain blackness until actual DrawBuffer call
		//****Problem regarding exporting
		//If your next drawcall is bound to attachmento , you will cleanse the default screen before swapping, and then write data to attachment0
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
		if(c.NDC_swap.IsOn()) {c.rtm.ChangeState(Drawable);c.NDC_swap.TurnOff();Utils.getErrors(gl,"Swapping Section");}
		for(int i=0;i<m.GetNumMeshes();i++) {
			m.GetMesh(i).SwitchShader(Drawable,c);
			m.GetMesh(i).Update(Drawable, c);
			m.GetMesh(i).DrawBuffer(Drawable);
		}
		//if(c.NDC_shader.IsOn()) {	c.NDC_shader.TurnOff();}//It's okay to turn NDC off for each drawcall without if .Because it is a toggle rather than a state which may cause the state to change right after a drawcall and quit that state
		//Now it's not okay because we expect a state
		if(c.NDC_export.IsOn()) {c.rtm.Export(Drawable);c.NDC_export.TurnOff();Utils.getErrors(gl,"Exporting Section");}
		Utils.getErrorsMultipleLines(gl,"Drawcall");
	}
}
