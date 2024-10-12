package jogl3.headers;

import assimp.AiMesh;
import assimp.AiNode;

public interface iModel {
	public void LoadScene(String path);
	public AiMesh sGetMesh(AiNode self,int index);
	public AiNode sGetNode(AiNode self,int index);
	public void ProcessMesh(AiMesh mesh);
	public void ProcessNode(AiNode node);
	public void LoadIntoMeshes(iMesh m);
	public iMesh GetMesh(int index);
}
