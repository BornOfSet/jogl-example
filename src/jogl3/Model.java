package jogl3;

import static assimp.AiPostProcessStep.Triangulate;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import assimp.AiMesh;
import assimp.AiNode;
import assimp.AiScene;
import assimp.Importer;
import glm_.vec3.Vec3;

import jogl3.headers.iModel;
import jogl3.headers.iMesh;

public class Model implements iModel{

	private int FaceSize = 3;
	private ArrayList<AiMesh> MeshesLookup;
	private List<iMesh> Meshes = new ArrayList<iMesh>();
	private static class VERTEX{public static VERTEX inst = new VERTEX();}
	private static class FACE{public static FACE inst = new FACE();}
	
	private FloatBuffer CreateBuffer(VERTEX _mode ,AiMesh mesh) {
		//struct Vertex{
		//		Vec3 pos;
		//		Vec3 normal;
		//		float[2] uv;
		//}
		//struct Vec3{
		//		float x;
		//		float y;
		// 	float z;
		//}
		int num = mesh.getNumVertices();
		int size = Vec3.length*2 + 2; 
		List<Vec3> pos = mesh.getVertices();
		List<Vec3> norm = mesh.getNormals(); //Hardnormals split vertices at the same position . UV do as well
		//------UNSTABLE EXPERIMENTAL UV FUNCTION
		//如果模型没有UV：Index Out of Bound
			List<float[]> uv = mesh.getTextureCoords().get(0);
			if(!mesh.hasTextureCoords(0)) {System.err.print("NO UV COORD IS FOUND\n");}
			if(uv.size()!= num) {System.err.print("INCOMPLETE UV COORD IS FOUND\n");}
		FloatBuffer output = FloatBuffer.allocate(num*size); 
		for(int i = 0;i<num;i++) {
			Vec3 v1 = pos.get(i);
			Vec3 v2 = norm.get(i);
			output.put(v1.getX());
			output.put(v1.getY());
			output.put(v1.getZ());
			output.put(v2.getX());
			output.put(v2.getY());
			output.put(v2.getZ());
			
			float[] v3 = uv.get(i);
			output.put(v3[0]);
			output.put(v3[1]);
		}
		output.flip();
		return output;
	}
	
	private IntBuffer CreateBuffer(FACE _mode ,AiMesh mesh) {
		//struct Faces{
		//		List<Integer> Face
		//}
		//struct List<Integer>{
		//		int A;
		//		int B;
		//		int C;
		//}
		int num = mesh.getNumFaces();
		int size = this.FaceSize;
		List<List<Integer>> faces = mesh.getFaces();
		IntBuffer output = IntBuffer.allocate(num*size); 
		for(int i = 0;i<num;i++) {
			List<Integer> face = faces.get(i);
			for(int j = 0;j<size;j++) {
				output.put(face.get(j));
			}
		}
		output.flip();
		return output;
	}
	
	@Override
	public void LoadScene(String path) {
		AiScene scene = new Importer().readFile(path , Triangulate.i);
		this.MeshesLookup = scene.getMeshes();
		ProcessNode(scene.rootNode);
	}

	@Override
	public AiMesh sGetMesh(AiNode self,int index) {
		int refs[] = self.getMeshes();
		return  MeshesLookup.get(refs[index]);
	}

	@Override
	public AiNode sGetNode(AiNode self,int index) {
		List<AiNode> Childrens = self.getChildren();
		return Childrens.get(index);
	}

	@Override
	public void ProcessMesh(AiMesh mesh) {
		FloatBuffer b1 = CreateBuffer(VERTEX.inst,mesh);
		IntBuffer b2 = CreateBuffer(FACE.inst,mesh);
		Mesh output = new Mesh();
		output.ImportVI(b1, b2);
		LoadIntoMeshes(output);
	}

	@Override
	public void ProcessNode(AiNode node) {
		int num = node.getNumMeshes();
		for(int i=0;i<num;i++) {
			ProcessMesh(sGetMesh(node,i));
		}
		num = node.getNumChildren();
		for(int i=0;i<num;i++) {
			ProcessNode(sGetNode(node,i));
		}
	}

	@Override
	public void LoadIntoMeshes(iMesh m) {
		Meshes.add(m);
	}

	@Override
	public iMesh GetMesh(int index) {
		return Meshes.get(index);
	}
	
	public int GetNumMeshes() {
		return Meshes.size();
	}
}
