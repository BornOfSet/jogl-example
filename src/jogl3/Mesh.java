package jogl3;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;

import glm_.glm;
import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import jogl3.headers.iMesh;

public class Mesh implements iMesh{
	private Buffer vertexBuffer;
	private Buffer indexBuffer;
	private IntBuffer VAO;
	private IntBuffer VBO;
	private IntBuffer EBO;
	
	private int numVAO=1;
	private int numVBO=1;
	private int numEBO=1;
	
	private int Fsize = Float.BYTES;
	private int Isize = Integer.BYTES;
	private int VertexNum;
	private int IndexNum;
	
	private int pCount = 3; //position is a float3
	private int nCount = 3;//normal is a float3
	private int tCount = 2;//uv is a float2
	
	String[] VS = Shaders.VS;
	String[] PS = Shaders.PS;
	
	String[] sec_VS = Shaders.VS_UV;
	String[] sec_PS = Shaders.PS_UV;
	
	private int prgm;
	private int vsRef; 
	private int psRef; 
	
	private int sec_p;
	private int sec_vsRef;
	private int sec_psRef;
	
	@Override
	public void ImportVI(Buffer vertex, Buffer index) {
		this.vertexBuffer = vertex;
		this.indexBuffer = index;
		VertexNum = this.vertexBuffer.capacity();
		IndexNum = this.indexBuffer.capacity();
	}

	@Override
	public void SetupContext(GLAutoDrawable drawable) {
		
		GL3 gl = drawable.getGL().getGL3();
		VAO = IntBuffer.allocate(numVAO);
		VBO = IntBuffer.allocate(numVBO);
		EBO = IntBuffer.allocate(numEBO);
		gl.glGenVertexArrays(numVAO, VAO);
		gl.glGenBuffers(numVBO, VBO);
		gl.glGenBuffers(numEBO, EBO);

		gl.glBindVertexArray(VAO.get(0));
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO.get(0));
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, EBO.get(0));	
		
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, 						 VertexNum * Fsize, 		vertexBuffer, 		GL3.GL_STATIC_DRAW);
		gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,    IndexNum  *  Isize,		indexBuffer, 		GL3.GL_STATIC_DRAW);
		
		gl.glEnableVertexAttribArray(0);
		gl.glEnableVertexAttribArray(1);
		gl.glEnableVertexAttribArray(2);

		gl.glVertexAttribPointer(0, pCount, GL3.GL_FLOAT, false, (pCount+nCount+tCount)*Fsize, 0);
		gl.glVertexAttribPointer(1, nCount, GL3.GL_FLOAT, false, (pCount+nCount+tCount)*Fsize, pCount*Fsize);
		gl.glVertexAttribPointer(2, tCount, GL3.GL_FLOAT, false, (pCount+nCount+tCount)*Fsize, pCount*Fsize+nCount*Fsize);
		
		gl.glBindVertexArray(0);
		
		prgm = gl.glCreateProgram();
		vsRef = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		psRef = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		gl.glShaderSource(vsRef, VS.length, VS, null);
		gl.glShaderSource(psRef, PS.length, PS, null);
		
		gl.glCompileShader(vsRef);
		gl.glCompileShader(psRef);
		
		//声明我们想要用哪些着色器产生可执行文件
		//可以拥有多个同阶段着色器，只要这么多着色器拼起来能组成完整的代码
		gl.glAttachShader(prgm, vsRef);
		gl.glAttachShader(prgm, psRef);
		
	    gl.glLinkProgram(prgm);//replaced with the programmable processors
	    //When a program object has been successfully linked, the program object can be made part of current state by calling glUseProgram
	    //一个程序是多个着色器的集合，可以说一个程序就是一个渲染管线。使用这个程序就是切入这一个渲染管线，与此同时你可能有很多个
	    //Link根据已经成功编译的着色器生成可执行的处理器，并且安装在管线对应的位置，这个过程中会分配并初始化Uniform。因此可知Uniform是对渲染程序而言的。这里的程序指的是环节，流程，也就是说连续单向地顶点——几何——片段
	    //可以在USE之后LINK。这样产生的可执行文件自动装载进当前的上下文。USE的本质上是在切换使用哪些可执行处理器
	   //默认状态下OPENGL是没有设置管线的，或者使用某种默认管线。也就是说你必须分配程序，才能拥有渲染管线
	    gl.glUseProgram(prgm);
	    
	    //Create another pipeline
	    CreateSecondShader(drawable);
	    
	    Mat4 mat = new Mat4(1.0f);
	    FloatBuffer fb = Utils.GetBuffer4x4(mat);
	    
		int u1 = gl.glGetUniformLocation(prgm, "view");
		gl.glUniformMatrix4fv(u1, 1, false, fb);	//Errors with the same type but that are generated different places are output as single one. GL_INVALID_OPERATION is generated if there is no current program object.
		int u2 = gl.glGetUniformLocation(prgm, "scale");
		gl.glUniformMatrix4fv(u2, 1, false, fb);	
		
		FloatBuffer L = FloatBuffer.allocate(3);
		L.put(1);L.put(1);L.put(0);
		L.flip();
		int u3 = gl.glGetUniformLocation(prgm, "light");
		gl.glUniform3fv(u3, 1, L);
	}

	@Override
	public void DrawBuffer(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();
	    gl.glBindVertexArray(VAO.get(0));
	    gl.glDrawElements(GL3.GL_TRIANGLES, IndexNum , GL3.GL_UNSIGNED_INT, 0);
	    gl.glBindVertexArray(0);
	}
	
	@Override
	public void Update(GLAutoDrawable drawable , Controller c) {
		GL3 gl = drawable.getGL().getGL3();
		if(c.NDC_shader.IsOff()) {

			Vec3 UP = new Vec3(0,1,0);
			Vec3 CameraVector = new Vec3(Math.sin(c.yaw),Math.sin(c.pitch),Math.cos(c.yaw));
			Vec3 CameraRight = UP.cross(CameraVector);
			Vec3 CameraUp = CameraVector.cross(CameraRight);
			Vec3 CameraLocation = c.trans;
					
			//摄像机基本原理
			//设空间内两点V01, V02，V02 + V21 = V01
			//设Vr = cross(V21，(0,1,0))
			//设Vup = cross(V21，Vr)
			//组建矩阵M = 
			//Vr,   Tx
			//Vup, Ty
			//V21, Tz
			//0,0,0,1
			//当Tx=Ty=Tz且M为单位矩阵时，称物体为静止
			//单位矩阵意味：
			//V21 = (0,0,1)；所以
			//X02 + X21 = X01		Y02 + Y21 = Y01		Z02 + Z21 = Z01
			//X02		0		X01		Y02		0	 	 Y01		Z02		1		Z01
			//X02 = X01 = X;
			//Y02 = Y01 = Y;
			//Z02 = Z01 - 1
			
			Mat4 M = new Mat4(
					new Vec4(CameraRight , CameraLocation.getX()),
					new Vec4(CameraUp , CameraLocation.getY()),
					new Vec4(CameraVector , CameraLocation.getZ()),
					new Vec4(0,0,0,1)
					);
			M = M.transpose();
			//Utils.PrintMatrix4x4(M);
			// This function returns -1 if name does not correspond to an active uniform variable in program
			//GLSL compilers and linkers try to be as efficient as possible. Therefore, they do their best to eliminate code that does not affect the stage outputs. Because of this, a uniform defined in a shader file does not have to be made available in the linked program. It is only available if that uniform is used by code that affects the stage output, and that the uniform itself can change the output of the stage.
			//Therefore, a uniform that is exposed by a fully linked program is called an "active" uniform; any other uniform specified by the original shaders is inactive. Inactive uniforms cannot be used to do anything in a program.
			int u1 = gl.glGetUniformLocation(prgm, "view");
			int u2 = gl.glGetUniformLocation(prgm, "scale");
			
			Mat4 Ms = new Mat4(1);
			Ms = glm.INSTANCE.scale(Ms, c.scale);
			FloatBuffer V = Utils.GetBuffer4x4(M);
			FloatBuffer S = Utils.GetBuffer4x4(Ms);
			//glUniform operates on the program object that was made part of current state by calling glUseProgram.
			//There would be a INVALID OPERATION ERROR if you're using inactive uniforms
			//Though they're are for future use of free floating camera
			gl.glUniformMatrix4fv(u1, 1, false, V);			
			gl.glUniformMatrix4fv(u2, 1, false, S);	
			
			c.CalculateNewLightLocation();
			FloatBuffer L = FloatBuffer.allocate(3);
			L.put(c.lightDirection.getX());L.put(c.lightDirection.getY());L.put(c.lightDirection.getZ());
			L.flip();
			int u3 = gl.glGetUniformLocation(prgm, "light");
			gl.glUniform3fv(u3, 1, L);
		}else {
				//The other Shader//
				////////////////////
			c.CalculateNewLightLocation();
			FloatBuffer L = FloatBuffer.allocate(3);
			L.put(c.lightDirection.getX());L.put(c.lightDirection.getY());L.put(c.lightDirection.getZ());
			L.flip();
			int u3 = gl.glGetUniformLocation(sec_p, "light");
			gl.glUniform3fv(u3, 1, L);
		}
	}

	private void CreateSecondShader(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();
		sec_p = gl.glCreateProgram();
		sec_vsRef = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		sec_psRef = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		gl.glShaderSource(sec_vsRef, sec_VS.length, sec_VS, null);
		gl.glShaderSource(sec_psRef, sec_PS.length, sec_PS, null);
		
		gl.glCompileShader(sec_vsRef);
		gl.glCompileShader(sec_psRef);
		
		gl.glAttachShader(sec_p, sec_vsRef);
		gl.glAttachShader(sec_p, sec_psRef);
		
	    gl.glLinkProgram(sec_p);
	}
	
	@Override
	public void SwitchShader(GLAutoDrawable drawable, Controller c) {
		GL3 gl = drawable.getGL().getGL3();
		if(c.NDC_shader.IsOn()) {//UV_MODE
			gl.glUseProgram(sec_p);//install into render state
		}else {
			 gl.glUseProgram(prgm);
		}
	}

	@Override
	public Buffer GetVertexBuffer() {
		// TODO Auto-generated method stub
		return this.vertexBuffer;
	}

	@Override
	public Buffer GetIndexBuffer() {
		// TODO Auto-generated method stub
		return this.indexBuffer;
	}
}
