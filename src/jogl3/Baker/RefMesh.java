package jogl3.Baker;

import static assimp.AiPostProcessStep.Triangulate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jogamp.opengl.GL3;

import assimp.AiMesh;
import assimp.AiScene;
import assimp.Importer;
import glm_.vec3.Vec3;
import jogl3.Trace.ShaderLoader;

public class RefMesh {
	
	private class IfMachine{
		float midX;
		float midY;
		float midZ;
		float d4Xleft;
		float d4Xright;
		float d4Yleft;
		float d4Yright;
		float d4Zleft;
		float d4Zright;
		int find = -1;
		float X;
		float Y;
		float Z;
		public IfMachine(float midX,float d4Xright,float d4Xleft,
									  float midY,float d4Yright,float d4Yleft,
									  float midZ,float d4Zright,float d4Zleft) {
			this.midX=midX;       
			this.midY=midY;       
			this.midZ=midZ;       
			this.d4Xleft=d4Xleft;    
			this.d4Xright=d4Xright;   
			this.d4Yleft=d4Yleft;    
			this.d4Yright=d4Yright;   
			this.d4Zleft=d4Zleft;    
			this.d4Zright=d4Zright;   
		}
		public int Start(float X , float Y , float Z) {
			this.X = X;
			this.Y = Y;
			this.Z = Z;
			if(X>midX) {
				if(X>d4Xright) {
					find = 48;
					return IfY();
				}
				else if(X<=d4Xright) {
					find = 32;
					return IfY();
				}
			}
			else if(X<=midX) {
				if(X>d4Xleft) {
					find = 16;
					return IfY();
				}
				else if(X<=d4Xleft) {
					find = 0;
					return IfY();
				}					
			}
			return -3;
		}
		private int IfY() {
			if(Y>midY) {
				if(Y>d4Yright) {
					find += 12;
					return IfZ();
				}
				else if(Y<=d4Yright) {
					find += 8;
					return IfZ();
				}
			}
			else if(Y<=midY) {
				if(Y>d4Yleft) {
					find += 4;
					return IfZ();
				}
				else if(Y<=d4Yleft) {
					find += 0;
					return IfZ();
				}					
			}
			return -2;
		}
		private int IfZ() {
			if(Z>midZ) {
				if(Z>d4Zright) {
					find += 3;
					return find;
				}
				else if(Z<=d4Zright) {
					find += 2;
					return find;
				}
			}
			else if(Z<=midZ) {
				if(Z>d4Zleft) {
					find += 1;
					return find;
				}
				else if(Z<=d4Zleft) {
					find += 0;
					return find;
				}					
			}
			return -1;
		}
	}

	private class Triangle{
		
		Vec3 A_Position;
		Vec3 B_Position;
		Vec3 C_Position;
		
		Vec3 A_Normal;
		Vec3 B_Normal;
		Vec3 C_Normal;
		
		public Triangle(Vec3 A, Vec3 B, Vec3 C) {
			this.A_Position = A;
			this.B_Position = B;
			this.C_Position = C;
		}
		
	}
	
	public void GenerateNormalForSpecificVertex(HashMap<Vec3,ArrayList<Vec3>> Mapping , Vec3 pA, Vec3 HardNormal) {
		if(Mapping.containsKey(pA)) {
			ArrayList<Vec3> Normals = Mapping.get(pA);
			Normals.add(HardNormal);
		}
		else {
			Mapping.put(pA, new ArrayList<Vec3>());
			Mapping.get(pA).add(HardNormal);	
		}
	}
	
	public Vec3 GenerateSmoothNormalForSpecificVertex(ArrayList<Vec3> BNormals) {
		//虽然这会导致奇点重复计算（同一个WP坐标在不同的三角形中平行存在，每个三角形在遇到它的时候都重新获取一次Hash表）
		//但是这能避免出现问题：新遍历到的三角形没法合成法线，从而导致X_Normal有缺口
		//如果要优化计算量，第一次算完后就清除List<Normal>然后添加avg作为唯一元素
		//最终我不是只输出一系列class Vertex{Vec3 Pos; Vec3 Nml}，而是输出具有三个顶点的Face，并且不用索引，因此不该省事别省事
		Vec3 Sum = new Vec3(0,0,0);
		for(int L = 0; L < BNormals.size();L++) {
			Sum = Sum.plus(BNormals.get(L));
		}
		Vec3 avg = Sum.div(BNormals.size());
		return avg;
	}
	
	float minX = Float.MAX_VALUE;
	float maxX = Float.MIN_VALUE;
	float minY = Float.MAX_VALUE;
	float maxY = Float.MIN_VALUE;
	float minZ = Float.MAX_VALUE;
	float maxZ = Float.MIN_VALUE;
	
	
	public void LoadFromPath(String path, GL3 gl) {
		//*初始化
		AiScene scene = new Importer().readFile(path , Triangulate.i);
		ArrayList<AiMesh> MeshCollection = scene.getMeshes();
		if(MeshCollection.size() > 1) {System.err.println("多于1个Mesh作为参考Mesh");}
		AiMesh HiddenMesh = MeshCollection.get(0);
		int VertexNum = HiddenMesh.getNumVertices();
		int FaceNum = HiddenMesh.getNumFaces();
		List<Vec3> Vertices = HiddenMesh.getVertices();
		List<List<Integer>> Faces = HiddenMesh.getFaces();
		System.out.print(FaceNum + "FaceNum");
		
		//*建立边界框		
		for(int i = 0;i < VertexNum;i++) {
			Vec3 position = Vertices.get(i);//一个表示位置的三浮点向量
			float posX = position.getX();
			float posY = position.getY();
			float posZ = position.getZ();
			minX = Math.min(minX, posX);
			minY = Math.min(minY, posY);
			minZ = Math.min(minZ, posZ);
			maxX = Math.max(maxX, posX);
			maxY = Math.max(maxY, posY);
			maxZ = Math.max(maxZ, posZ);
		}
		float midX = (minX + maxX)/2;
		float midY = (minY + maxY)/2;
		float midZ = (minZ + maxZ)/2;	
		float d4Xleft = (minX + midX)/2;
		float d4Xright = (maxX + midX)/2;
		float d4Yleft = (minY + midY)/2;
		float d4Yright = (maxY + midY)/2;
		float d4Zleft = (minZ + midZ)/2;
		float d4Zright = (maxZ + midZ)/2;
		
		//区分三角集
		ArrayList<ArrayList<Triangle>> TriangleSects_64 = new ArrayList<ArrayList<Triangle>>();
		for(int i = 0;i < 64;i++) {
			ArrayList<Triangle> Strip = new ArrayList<Triangle>();
			TriangleSects_64.add(Strip);
		}
		
		//顶点是一个vec3坐标和一个vec3法线的复合体，只有确定了坐标-法线的映射关系，才能构建一个顶点。Mapping是顶点的一个集合
		HashMap<Vec3,ArrayList<Vec3>> Mapping = new HashMap<Vec3,ArrayList<Vec3>>();
		//分配三角形到三角集中
		for(int i = 0;i < FaceNum;i++) {
			//获知世界坐标
			List<Integer> Face = Faces.get(i);
			int pAi = Face.get(0);  
			int pBi = Face.get(1);  
			int pCi = Face.get(2);  
			Vec3 pA = Vertices.get(pAi);
			Vec3 pB = Vertices.get(pBi);
			Vec3 pC = Vertices.get(pCi);
			//计算平坦法线
			Vec3 v1 = pA.minus(pB);
			Vec3 v2 = pC.minus(pB);
			Vec3 HardNormal = v1.cross(v2);
			//写入顶点坐标的一对多法线关系
			GenerateNormalForSpecificVertex(Mapping, pA, HardNormal);
			GenerateNormalForSpecificVertex(Mapping, pB, HardNormal);
			GenerateNormalForSpecificVertex(Mapping, pC, HardNormal);
			//三角形分配给对应的三角集，统一管理
			Triangle tri = new Triangle(pA,pB,pC);
			float px_min = Math.min( Math.min( pA.getX() , pB.getX() ), pC.getX() );
			float py_min = Math.min( Math.min( pA.getY() , pB.getY() ), pC.getY() );
			float pz_min = Math.min( Math.min( pA.getZ() , pB.getZ() ), pC.getZ() );
			IfMachine newMachine = new IfMachine(  midX, d4Xright, d4Xleft,
																				   midY, d4Yright, d4Yleft,
																				   midZ, d4Zright,  d4Zleft);
			int find = newMachine.Start(px_min, py_min, pz_min);
			ArrayList<Triangle> Strip = TriangleSects_64.get(find);
			Strip.add(tri);
		}
		
		//face_num*3 = ver_num
		//ver_num*3 = float_num
		FloatBuffer PositionBuffer = FloatBuffer.allocate(FaceNum * 3 * 3);
		FloatBuffer NormalBuffer = FloatBuffer.allocate(FaceNum * 3 * 3);
		//WPTBO和NMTBO共同适用的64区划起点位置索引
		IntBuffer LayoutPointers = IntBuffer.allocate(64);
		int accumulate = 0;
		for(int i=0;i < 64;i++) {
			ArrayList<Triangle> Sect = TriangleSects_64.get(i);
			accumulate += Sect.size();
			LayoutPointers.put(accumulate);
			
			//以三角形为基准遍历，合并经过的每一个WP上的法线束，紧接着-或者如果List容量为1的时候-将单一法线添加到对应名字_Normal的Vec3槽位中
			//这个程序很简单，弄清楚你要不要把不同三角形内出现的重复顶点（在另一个三角形内出现过的顶点）合并，你就知道该怎么写了
			for(int k=0;k < Sect.size();k++) {
				Triangle tri = Sect.get(k);
				
				ArrayList<Vec3> ANormals = Mapping.get(tri.A_Position);
				tri.A_Normal = GenerateSmoothNormalForSpecificVertex(ANormals);
				
				ArrayList<Vec3> BNormals = Mapping.get(tri.B_Position);
				tri.B_Normal = GenerateSmoothNormalForSpecificVertex(BNormals);
				
				ArrayList<Vec3> CNormals = Mapping.get(tri.C_Position);
				tri.C_Normal = GenerateSmoothNormalForSpecificVertex(CNormals);
				
				PositionBuffer.put(tri.A_Position.getArray());
				PositionBuffer.put(tri.B_Position.getArray());
				PositionBuffer.put(tri.C_Position.getArray());
				
				NormalBuffer.put(tri.A_Normal.getArray());
				NormalBuffer.put(tri.B_Normal.getArray());
				NormalBuffer.put(tri.C_Normal.getArray());
			}
		}
		PositionBuffer.flip();
		NormalBuffer.flip();
		LayoutPointers.flip();
		
		IntBuffer VBOs = IntBuffer.allocate(2);
		gl.glGenBuffers(2, VBOs);
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBOs.get(0));
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, PositionBuffer.capacity()*Float.BYTES, PositionBuffer, GL3.GL_STATIC_DRAW);
		//PositionBuffer
		IntBuffer Textures = IntBuffer.allocate(2);
		gl.glGenTextures(2, Textures);
		gl.glActiveTexture(GL3.GL_TEXTURE0);
		gl.glBindTexture(GL3.GL_TEXTURE_BUFFER, Textures.get(0));
		gl.glTexBuffer(GL3.GL_TEXTURE_BUFFER, 			GL3.GL_RGB32F, 				VBOs.get(0));
		//NormalBuffer
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, VBOs.get(1));
		gl.glBufferData(GL3.GL_ARRAY_BUFFER, NormalBuffer.capacity()*Float.BYTES, NormalBuffer, GL3.GL_STATIC_DRAW);
		gl.glActiveTexture(GL3.GL_TEXTURE0 + 1);
		gl.glBindTexture(GL3.GL_TEXTURE_BUFFER, Textures.get(1));
		gl.glTexBuffer(GL3.GL_TEXTURE_BUFFER, 			GL3.GL_RGB32F, 				VBOs.get(1));
		
		//Shader
		int program = gl.glCreateProgram();
		int Vshader = gl.glCreateShader(GL3.GL_VERTEX_SHADER);
		int Pshader = gl.glCreateShader(GL3.GL_FRAGMENT_SHADER);
		String Vsource[] = new String[1];
		String Psource[] = new String[1];
		try {
			Vsource[0] = ShaderLoader.LoadShader("VS.txt");
			Psource[0] = ShaderLoader.LoadShader("trace3.fs");
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
	}
	
}
