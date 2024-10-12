package jogl3.Trace;

import static assimp.AiPostProcessStep.Triangulate;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;

import assimp.AiMesh;
import assimp.AiScene;
import assimp.Importer;
import glm_.glm;
import glm_.mat4x4.Mat4;
import glm_.vec3.Vec3;
import glm_.vec4.Vec4;
import jogl3.Controller;
import jogl3.Utils;
import jogl3.headers.iMesh;

public class RayTraceProgram {
	/*
	ArrayList<Float> tree1 = new ArrayList<Float>();
	ArrayList<Float> tree2 = new ArrayList<Float>();
	ArrayList<Float> tree3 = new ArrayList<Float>();
	ArrayList<Float> tree4 = new ArrayList<Float>();
	ArrayList<Float> tree5 = new ArrayList<Float>();
	ArrayList<Float> tree6 = new ArrayList<Float>();
	ArrayList<Float> tree7 = new ArrayList<Float>();
	ArrayList<Float> tree8 = new ArrayList<Float>();
	
	ArrayList<ArrayList<Float>> tree = new ArrayList<ArrayList<Float>>();
	*/
	
	ArrayList<ArrayList<Float>> tree = new ArrayList<ArrayList<Float>>();
	
	GLAutoDrawable drawable = null;
	boolean HasContext = false;
	
	IntBuffer VAO_QUAD;
	boolean VAO_QUAD_Initialized = false;
	int Location_QUAD = 0;
	
	IntBuffer VAO_OBJECT;  //目前他什么都不做。他的目的是给予在着色器中访问的几何体
	boolean VAO_OBJECT_Initialized = false;
	boolean VAO_OBJECT_EnableReplace = false; //这个标志在TRACE_TARGET_区块下方检查，如果允许覆盖，那么将VAO_OBJECT替换为新执行的CreateGeo产生的几何体；否则报告错误，CreateGeo重复执行
	
	boolean draw_allowed = false;
	
	int program;//由于没有第二个程序，该接口无所谓开放与否
	
	int Draw_size;
	
	IntBuffer layout;
	float shader_minX;
	float shader_maxX;
	float shader_minY;
	float shader_maxY;
	float shader_minZ;
	float shader_maxZ;
	
	public RayTraceProgram() {
		/*
		tree.add(tree1);
		tree.add(tree2);
		tree.add(tree3);
		tree.add(tree4);
		tree.add(tree5);
		tree.add(tree6);
		tree.add(tree7);
		tree.add(tree8);*/
		for(int i=0;i<64;i++) {
			ArrayList<Float> tree1 = new ArrayList<Float>();
			tree.add(tree1);
		}
	}
	
	public void CreateGeo(String mode) {
		if(this.HasContext == false) {return;}
		GL3 gl = this.drawable.getGL().getGL3();
		if(mode == "QUAD") {
			float plane[]= {-1,-1, //left bottom
									  -1,1,
									  1,-1,
									  -1,1,
									  1,-1,
									  1,1 //right top
									  };
			FloatBuffer Transmit = FloatBuffer.wrap(plane);
			IntBuffer QuadVAO = IntBuffer.allocate(1);
			gl.glGenVertexArrays(1, QuadVAO);
			gl.glBindVertexArray(QuadVAO.get(0));//目标：绑定所有内容，给出VAO引用，当使用引用的时候，就可以直接使用所有已经设定好的状态
			this.VAO_QUAD = QuadVAO;
			this.VAO_QUAD_Initialized = true;
			IntBuffer Attribute_Capacity = IntBuffer.allocate(1);
			gl.glGenBuffers(1, Attribute_Capacity);
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, Attribute_Capacity.get(0));
			gl.glBufferData(GL3.GL_ARRAY_BUFFER, Transmit.capacity()*Float.BYTES, Transmit, GL3.GL_STATIC_DRAW);
			gl.glEnableVertexAttribArray(Location_QUAD);
			gl.glVertexAttribPointer(Location_QUAD, 2, GL3.GL_FLOAT, false, 2*Float.BYTES, 0);
			gl.glBindVertexArray(0);
		}
		if(mode == "TRACE_TARGET_PROGRAM") {
			CreateSphere();
		}
		if(mode == "TRACE_TARGET_IMPORT") {
			Import();
			AiScene scene = new Importer().readFile("C:/Users/User/Desktop/raytest.obj" , Triangulate.i);
			ArrayList<AiMesh> MeshesLookup = scene.getMeshes();
			int vertex_size = 0;
			int face_size = 0;
			for(int i = 0;i < MeshesLookup.size();i++) {
				vertex_size += MeshesLookup.get(i).getNumVertices();
				face_size += MeshesLookup.get(i).getNumFaces();
			}
			System.out.print(vertex_size + "vertex_size");
			System.out.print(face_size + "face_size");
			FloatBuffer Pos_Norm_PerVertex = FloatBuffer.allocate(vertex_size*6);
			IntBuffer faceindex = IntBuffer.allocate(face_size*3); 
			float minX = Float.MAX_VALUE;
			float maxX = Float.MIN_VALUE;
			float minY = Float.MAX_VALUE;
			float maxY = Float.MIN_VALUE;
			float minZ = Float.MAX_VALUE;
			float maxZ = Float.MIN_VALUE;
			Draw_size =  faceindex.capacity();
			System.out.print( MeshesLookup.size() + " MeshesLookup.size()");
			for(int i = 0;i < MeshesLookup.size();i++) {
				int size = MeshesLookup.get(i).getNumVertices();
				int Fsize = MeshesLookup.get(i).getNumFaces();
				List<Vec3> all_position = MeshesLookup.get(i).getVertices();
				List<Vec3> all_normal = MeshesLookup.get(i).getNormals();
				List<List<Integer>> faces = MeshesLookup.get(i).getFaces();
				for(int j = 0;j < size;j++) {
					Vec3 glm_vector = all_position.get(j);
					Vec3 glm_vector_N = all_normal.get(j);
					//Bounding Box
					minX = Math.min(minX, glm_vector.getX());
					minY = Math.min(minY, glm_vector.getY());
					minZ = Math.min(minZ, glm_vector.getZ());
					maxX = Math.max(maxX, glm_vector.getX());
					maxY = Math.max(maxY, glm_vector.getY());
					maxZ = Math.max(maxZ, glm_vector.getZ());
					//
					Pos_Norm_PerVertex.put(glm_vector.getX());
					Pos_Norm_PerVertex.put(glm_vector.getY());
					Pos_Norm_PerVertex.put(glm_vector.getZ());
					Pos_Norm_PerVertex.put(glm_vector_N.getX());
					Pos_Norm_PerVertex.put(glm_vector_N.getY());
					Pos_Norm_PerVertex.put(glm_vector_N.getZ());
					//System.out.println(glm_vector.toString() + "P-N	" + glm_vector_N.toString() + "N-T	" + MeshesLookup.get(i).getTextureCoords().get(0).get(j)[0] + "," + MeshesLookup.get(i).getTextureCoords().get(0).get(j)[1]);
				}
				for(int j = 0;j < Fsize;j++) {
					List<Integer> face = faces.get(j);
					for(int x = 0;x<3;x++) {
						faceindex.put(face.get(x));
					}
				}
			}		
			Pos_Norm_PerVertex.flip();
			faceindex.flip();
			
			IntBuffer VAO = IntBuffer.allocate(1);
			IntBuffer VBO = IntBuffer.allocate(1);
			IntBuffer EBO = IntBuffer.allocate(1);
			
			gl.glGenVertexArrays(1, VAO);

			gl.glGenBuffers(1, VBO);
			gl.glGenBuffers(1, EBO);

			gl.glBindVertexArray(VAO.get(0));
			VAO_OBJECT = VAO;
			VAO_OBJECT_Initialized = true;
			
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBO.get(0));
			gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, EBO.get(0));	
			
			gl.glBufferData(GL3.GL_ARRAY_BUFFER, 						 Pos_Norm_PerVertex.capacity() * Float.BYTES, 		Pos_Norm_PerVertex, 		GL3.GL_STATIC_DRAW);
			gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,    faceindex.capacity() * Integer.BYTES,						faceindex, 							GL3.GL_STATIC_DRAW);
			
			gl.glEnableVertexAttribArray(2);
			gl.glEnableVertexAttribArray(1);

			gl.glVertexAttribPointer(2, 3, GL3.GL_FLOAT, false, (3+3)*Float.BYTES, 0);
			gl.glVertexAttribPointer(1, 3, GL3.GL_FLOAT, false, (3+3)*Float.BYTES, 3*Float.BYTES);
			
			gl.glBindVertexArray(0);
			
			////////////////////////以上部分可以正常运行/////////////
			/*
			int indicator = 3;
			boolean ReadingNormal = true;
			float midX = (minX + maxX)/2;
			float midY = (minY + maxY)/2;
			float midZ = (minZ + maxZ)/2;
			for(int i = 0;i<Pos_Norm_PerVertex.capacity();i++) {
				float component = Pos_Norm_PerVertex.get(i);//won't change the position indicator
				if (indicator == 3) {indicator=0; ReadingNormal = !ReadingNormal;}//0,1,2
				if (ReadingNormal == false) {
					//Reading Position
					switch(indicator) {//Ideally this should be for(x){for(y){for(z){}}}   //Now you wish to calculate unions of all these 3 dualistic divisions
					case 0:	//component is X component
						break;
					case 1:		//component is Y component
						break;
					case 2:		//component is Z component
						break;
					}
				}else {
					//Reading Normal
					
				}
				indicator ++;
			}*/
			/////////for each X do for each Y do for each Z
			
			
			/*
			float midX = (minX + maxX)/2;
			float midY = (minY + maxY)/2;
			float midZ = (minZ + maxZ)/2;	
			int count = 0;
			while(count < Pos_Norm_PerVertex.capacity()) {
				//对这一部分上的每次“经过”，保证当它经过的时候，内部状态count在0,1,2之中
				//tree(Pos_Norm_PerVertex.get(count), Pos_Norm_PerVertex.get(count+1), Pos_Norm_PerVertex.get(count+2));//XYZ
				float X = Pos_Norm_PerVertex.get(count);
				float Y = Pos_Norm_PerVertex.get(count+1);
				float Z = Pos_Norm_PerVertex.get(count+2);
				if(X<midX) {
					if(Y<midY) {
						if(Z<midZ) {
							tree(X,Y,Z,1); //-X -Y -Z
						}
						else if(Z>=midZ) {
							tree(X,Y,Z,2); //-X -Y Z
						}
					}
					else if(Y>=midY) {
						if(Z<midZ) {
							tree(X,Y,Z,3); //-X Y -Z
						}
						else if(Z>=midZ) {
							tree(X,Y,Z,4); //-X Y Z
						}
					}
				}
				else if(X>=midX) {
					if(Y<midY) {
						if(Z<midZ) {
							tree(X,Y,Z,5); //X -Y -Z
						}
						else if(Z>=midZ) {
							tree(X,Y,Z,6); //X -Y Z
						}
					}
					else if(Y>=midY) {
						if(Z<midZ) {
							tree(X,Y,Z,7); //X Y -Z
						}
						else if(Z>=midZ) {
							tree(X,Y,Z,8); //X Y Z
						}
					}
				}
				count += 6;//skip these 3 position values as well as 3 normal values
			}*/
			shader_minX = minX;
			shader_maxX = maxX;
			shader_minY = minY;
			shader_maxY = maxY;
			shader_minZ = minZ;
			shader_maxZ = maxZ;
			float midX = (minX + maxX)/2;
			float midY = (minY + maxY)/2;
			float midZ = (minZ + maxZ)/2;	
			
			float d4Xleft = (minX + midX)/2;
			float d4Xright = (maxX + midX)/2;
			float d4Yleft = (minY + midY)/2;
			float d4Yright = (maxY + midY)/2;
			float d4Zleft = (minZ + midZ)/2;
			float d4Zright = (maxZ + midZ)/2;/*
			System.out.println(minX+"	minX");
			System.out.println(maxX+"	maxX");
			System.out.println(minY+"	minY");		
			System.out.println(maxY+"	maxY");		
			System.out.println(midX+"	midX");
			System.out.println(midY+"	midY");
			System.out.println(midZ+"	midZ");
			System.out.println(d4Xleft+"	d4Xleft");
			System.out.println(d4Xright+"	d4Xright");
			System.out.println(d4Yleft+"d4Yleft");
			System.out.println(d4Yright+"	d4Yright");
			System.out.println(d4Zleft+"	d4Zleft");
			System.out.println(d4Zright+"	d4Zright");*/
			
			int count = 0;
			System.out.println(Pos_Norm_PerVertex.capacity());
			System.out.print( Pos_Norm_PerVertex.capacity() / 6 + " Pos_Norm_PerVertex.capacity() /6 顶点数");

			while(count < Pos_Norm_PerVertex.capacity()) {
				//对这一部分上的每次“经过”，保证当它经过的时候，内部状态count在0,1,2之中
				//tree(Pos_Norm_PerVertex.get(count), Pos_Norm_PerVertex.get(count+1), Pos_Norm_PerVertex.get(count+2));//XYZ
				int find = -1;
				float X = Pos_Norm_PerVertex.get(count);
				float Y = Pos_Norm_PerVertex.get(count+1);
				float Z = Pos_Norm_PerVertex.get(count+2);
				
				Vec3 pass = new Vec3(X,Y,Z);
				
				if(X>midX) {
					if(X>d4Xright) {
						find = 48;//0~15
						ifT(Y,midY,d4Yright,d4Yleft,Z,midZ,d4Zright,d4Zleft,find,pass);
					}
					else if(X<=d4Xright) {
						find = 32;//16~31
						ifT(Y,midY,d4Yright,d4Yleft,Z,midZ,d4Zright,d4Zleft,find,pass);
					}
				}
				else if(X<=midX) {
					if(X>d4Xleft) {
						find = 16;//32~47
						ifT(Y,midY,d4Yright,d4Yleft,Z,midZ,d4Zright,d4Zleft,find,pass);
					}
					else if(X<=d4Xleft) {
						find = 0;//48~63
						ifT(Y,midY,d4Yright,d4Yleft,Z,midZ,d4Zright,d4Zleft,find,pass);
					}					
				}
				count += 6;//skip current 3 position values as well as sequential 3 normal values
			}
			/*
			for(int i = 0;i < 64;i++) {
				float MAX = Float.MIN_VALUE;
				float MIN = Float.MAX_VALUE;
				for(int j = 0;j<tree.get(i).size();j++) {
					float p = tree.get(i).get(j);
					MIN = Math.min(MIN, p);
					MAX = Math.max(MIN, p);
				}
				System.out.print("\n");
				System.out.println(i + " section	MIN = " + MIN + "		MAX = " + MAX);
			}*/
			
			IntBuffer Texture = IntBuffer.allocate(1);
			gl.glGenTextures(1, Texture);
			gl.glActiveTexture(GL3.GL_TEXTURE0); //实际值为0，但是该枚举的值不为0。同理也应用于GL_MAX_TEXTURE_BUFFER_SIZE，实际值极大，枚举值不代表实际值
			gl.glBindTexture(GL3.GL_TEXTURE_BUFFER,Texture.get(0));
	
			IntBuffer Acceleration = IntBuffer.allocate(1);
			gl.glGenBuffers(1, Acceleration);
			gl.glBindBuffer(GL3.GL_TEXTURE_BUFFER, Acceleration.get(0));//Invalid Operation发生在向GL_TEXTURE_BUFFER中写入数据（glbufferdata导致）时GL_TEXTURE_BUFFER绑定点（target）上不存在“接受信号”用的buffer object
			//如果你想在glbufferdata中向GL_TEXTURE_BUFFER传入数据，首先你要在这里先把任一buffer object绑定在GL_TEXTURE_BUFFER上
			
			FloatBuffer reorder = FloatBuffer.allocate(vertex_size*3);//pos
			
			IntBuffer size = IntBuffer.allocate(64);
			int accumulate = 0;
			for(int i = 0;i < 64;i++) {
				int currentsize = tree.get(i).size() / 3; // Number of elements
				accumulate += currentsize;
				//System.out.println(" " + currentsize);
				size.put(accumulate);
				for(int j = 0;j<tree.get(i).size();j+=3) {
					float X = tree.get(i).get(j);
					float Y = tree.get(i).get(j+1);
					float Z = tree.get(i).get(j+2);
					reorder.put(X);
					reorder.put(Y);
					reorder.put(Z);
				}
			}
			reorder.flip();
			size.flip();
			layout = size;
			//size[i] is the new beginning location of the following group's first element
			//System.out.println(sizeK);//Position and Normal are separated
			System.out.println("" + reorder.capacity());
			gl.glBufferData(GL3.GL_TEXTURE_BUFFER, 		reorder.capacity() * Float.BYTES, 		reorder, 							GL3.GL_STATIC_DRAW);
			gl.glTexBuffer(GL3.GL_TEXTURE_BUFFER, 			GL3.GL_RGB32F, 								Acceleration.get(0));//The internalFormat​ : how the pixel data is stored in the buffer object 如何解析这一长串位
			System.out.println(d4Xleft-minX);
			System.out.println(midX-d4Xleft);
		}
	}
	
	public void ifT(float T, float midT, float d4Tright,float d4Tleft,float Z, float midZ, float d4Zright,float d4Zleft,int find,Vec3 pass) {
		if(T>midT) {
			if(T>d4Tright) {
				find += 12;//0~3
				ifT(Z,midZ,d4Zright,d4Zleft,find,pass);
			}
			else if(T<=d4Tright) {
				find += 8;//4~7
				ifT(Z,midZ,d4Zright,d4Zleft,find,pass);
			}
		}
		else if(T<=midT) {
			if(T>d4Tleft) {
				find += 4;//8~11
				ifT(Z,midZ,d4Zright,d4Zleft,find,pass);
			}
			else if(T<=d4Tleft) {
				find += 0;//12~15
				ifT(Z,midZ,d4Zright,d4Zleft,find,pass);
			}					
		}
	}
	
	public void ifT(float T, float midT, float d4Tright,float d4Tleft,int find, Vec3 pass) {
		float X = pass.getX();
		float Y = pass.getY();
		float Z = pass.getZ();
		if(T>midT) {
			if(T>d4Tright) {
				find += 3;
				tree.get(find).add(X);
				tree.get(find).add(Y);
				tree.get(find).add(Z);
				//System.out.println(find+"	find");
			}
			else if(T<=d4Tright) {
				find += 2;
				tree.get(find).add(X);
				tree.get(find).add(Y);
				tree.get(find).add(Z);
				//System.out.println(find+"	find");
			}
		}
		else if(T<=midT) {
			if(T>d4Tleft) {
				find += 1;
				tree.get(find).add(X);
				tree.get(find).add(Y);
				tree.get(find).add(Z);
				//System.out.println(find+"	find");
			}
			else if(T<=d4Tleft) {
				find += 0;
				tree.get(find).add(X);
				tree.get(find).add(Y);
				tree.get(find).add(Z);
				//System.out.println(find+"	find");
			}					
		}
	}
	
	public void CreateSphere() {}
	public void Import() {}
	public void SetImportPath() {}
	
	public void BindContext(GLAutoDrawable  arg) {
		this.drawable = arg;
		this.HasContext = true;
	}
	
	public void UnbindContext() {
		this.drawable = null;
		this.HasContext = false;
	}
	
	public void Drawable(boolean f) {
		this.draw_allowed  = f;
	}
	
	public boolean Drawable() {
		return this.draw_allowed;
	}
	 
	public void InstallShader() {
		if(this.HasContext == false) {return;}
		GL3 gl = this.drawable.getGL().getGL3();
		this.program = gl.glCreateProgram();
		int Vshader = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		int Pshader = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		String Vsource[] = new String[1];
		String Psource[] = new String[1];
		try {
			Vsource[0] = ShaderLoader.LoadShader("VS.txt");
			Psource[0] = ShaderLoader.LoadShader("trace3 - Copy.fs");
		} catch (IOException e) {
			e.printStackTrace();
		}
		gl.glShaderSource(Vshader, 1, Vsource, null);
		gl.glShaderSource(Pshader, 1, Psource, null);
		//-----------------------
		gl.glCompileShader(Vshader);
		ByteBuffer b = ByteBuffer.allocate(512);
		gl.glGetShaderInfoLog(Vshader, 512, null, b);
		byte[] ba = b.array();
		System.out.print("\n______VS Compile Error_____\n");
		System.out.println(new String(ba));
		//-----------------------
		gl.glCompileShader(Pshader);
		ByteBuffer b2 = ByteBuffer.allocate(512);
		gl.glGetShaderInfoLog(Pshader, 512, null, b2);
		byte[] ba2 = b2.array();
		System.out.print("\n______PS Compile Error_____\n");
		System.out.println(new String(ba2));
		//-----------------------
		gl.glAttachShader(program, Vshader);
		gl.glAttachShader(program, Pshader);
	    gl.glLinkProgram(program);
	    gl.glUseProgram(program);
	    
	    
		int u1 = gl.glGetUniformLocation(program, "TBOlayout");//使用[]来表明你要选择数组的元素，而非数组本身
		gl.glUniform1iv(u1, 64, layout);
		//for(int i=0;i<64;i++) {
			//System.out.println(layout.get(i));
		//}
		//System.out.print("------\n");

		int u2 = gl.glGetUniformLocation(program, "minX");
		gl.glUniform1f(u2, shader_minX);
		int u3 = gl.glGetUniformLocation(program, "maxX");
		gl.glUniform1f(u3, shader_maxX);
		int u4 = gl.glGetUniformLocation(program, "minY");
		gl.glUniform1f(u4, shader_minY);
		int u5 = gl.glGetUniformLocation(program, "maxY");
		gl.glUniform1f(u5, shader_maxY);
		int u6 = gl.glGetUniformLocation(program, "minZ");
		gl.glUniform1f(u6, shader_minZ);
		int u7 = gl.glGetUniformLocation(program, "maxZ");
		gl.glUniform1f(u7, shader_maxZ);
	}
	
	public void Display(String mode, Controller c) {
		if(this.HasContext == false) {return;}
		GL3 gl = this.drawable.getGL().getGL3();
		//如果VAO有值{......
		if (mode =="TRACE_TARGET") {
		    gl.glBindVertexArray(VAO_OBJECT.get(0));
		    gl.glDrawElements(GL3.GL_TRIANGLES, Draw_size , GL3.GL_UNSIGNED_INT, 0);
			gl.glBindVertexArray(0);
			Utils.getErrorsMultipleLines(gl,"TRACE");
		}
		if (mode =="QUAD") {
			Vec3 UP = new Vec3(0,1,0);
			Vec3 CameraVector = new Vec3(Math.sin(c.yaw),Math.sin(c.pitch),Math.cos(c.yaw));
			Vec3 CameraRight = UP.cross(CameraVector);
			Vec3 CameraUp = CameraVector.cross(CameraRight);
			Vec3 CameraLocation = c.trans;
			Mat4 M = new Mat4(
					new Vec4(CameraRight , CameraLocation.getX()),
					new Vec4(CameraUp , CameraLocation.getY()),
					new Vec4(CameraVector , CameraLocation.getZ()),
					new Vec4(0,0,0,1)
					);
			M = M.transpose();

			int u1 = gl.glGetUniformLocation(program, "view");
			FloatBuffer V = Utils.GetBuffer4x4(M);
			gl.glUniformMatrix4fv(u1, 1, false, V);			
			
		    gl.glBindVertexArray(VAO_QUAD.get(0));
			gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);
			gl.glBindVertexArray(0);
		}
	}
}
