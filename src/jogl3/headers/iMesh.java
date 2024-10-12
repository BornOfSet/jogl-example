package jogl3.headers;

import java.nio.Buffer;

import com.jogamp.opengl.GLAutoDrawable;

import jogl3.Controller;

public interface iMesh {
	public void ImportVI(Buffer vertex,Buffer index);
	public void SetupContext(GLAutoDrawable drawable);
	public void DrawBuffer(GLAutoDrawable drawable);
	public void Update(GLAutoDrawable drawable , Controller c);
	public void SwitchShader(GLAutoDrawable drawable , Controller c);
	public Buffer GetVertexBuffer();
	public Buffer GetIndexBuffer();
}
