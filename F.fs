#version 330 core
out vec4 Colors;
in vec3 Pos;
in vec3 Norm;
in vec2 UV;
uniform int Layout[64];
uniform float minX;
uniform float maxX;
uniform float minY;
uniform float maxY;
uniform float minZ;
uniform float maxZ;
uniform samplerBuffer SamplerP;
uniform samplerBuffer SamplerN;


struct HitSubBox{
	vec3 Position;
	int ID;
};

void main()
{
	float midX = (minX + maxX)/2;
	float midY = (minY + maxY)/2;
	float midZ = (minZ + maxZ)/2;
	float d4Xleft = (minX + midX)/2;
	float d4Xright = (maxX + midX)/2;
	float d4Yleft = (minY + midY)/2;
	float d4Yright = (maxY + midY)/2;
	float d4Zleft = (minZ + midZ)/2;
	float d4Zright = (maxZ + midZ)/2;

	float[5] _X = float[5](minX,d4Xleft,midX,d4Xright,maxX);
	float[5] _Y = float[5](minY,d4Yleft,midY,d4Yright,maxY);
	float[5] _Z = float[5](minZ,d4Zleft,midZ,d4Zright,maxZ);

	
	/*					
	 *  		╔───────╗
	 *		    ╠Options╣
	 * 			╚━━━━━━━╝
	 *  ╔───────────╗   ╔───────────╗  
	 *	╠低模包裹高模╣   ╠高模包裹低模╣
	 *  ╚━━━━━━━━━━━╝   ╚━━━━━━━━━━━╝
	 */
	vec3 RayStart = vec3(UV,-1);
	vec3 RaymarchDirection = vec3(0,0,1);
	float JumpTo = 0;
	float MAX_ITERATION = 20;
	float Threshold = 0.001;
	HitSubBox hit = HitSubBox(vec3(0),-1);
	float Mask = 0;

	for(int i=0;i<MAX_ITERATION;i++){
		float MinDistance = 9999999;
		for(int _x = 0;			_x < 4; _x++){
			for(int _y = 0;		_y < 4; _y++){
				for(int _z = 0;	_z < 4; _z++){
					int id = _x * 16 + _y * 4 + _z;
					vec3 BBmin = vec3(_X[_x],	_Y[_y],	  _Z[_z]);
					vec3 BBmax = vec3(_X[_x+1], _Y[_y+1], _Z[_z+1]);
					vec3 Center = (BBmin + BBmax)/2;
					vec3 BoundingSize = BBmax - BBmin;
					vec3 RelativeLocation = RayStart - Center;
					vec3 Destination = RelativeLocation + RaymarchDirection * JumpTo;
					vec3 VDist3 = abs(Destination) - BoundingSize/2;
					VDist3 = max(VDist3,vec3(0));
					float dist = length(VDist3);
					if(dist < MinDistance){
						MinDistance = dist;
						hit.ID = id;
						hit.Position = Destination;
					}
				}
			}
		}
		JumpTo = JumpTo + MinDistance;

		if(MinDistance < Threshold){
			Mask = 1 - MinDistance;
			break;
		}

		if(JumpTo > 1){
			break;
		}

	}

	Colors = vec4(normalize(hit.Position),1);
}