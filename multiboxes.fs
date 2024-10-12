#version 330 core
out vec4 Colors;
in vec2 pos;
uniform float time;		
		/*
		this = getcurrentFragment;
		dist = 999999;
		id = -1;
		OS = vec3(0,0,0);
		for each k,v in geometries do
			for each photon along path do
				location = vec3(X,Y,Z);
				collision = photon-location;
				dimension = vec3(W,H,D);
				OnSurface = location;
				photon_new_position_along_raypath = collision + RD*JT;
				vec3 distV = max(abs(photon_new_....)-dimension,vec3(0));
				float dist = length(distV);
				if(dist < E) then
					if(dist<this.dist) then
						this.dist = dist;
						this.OS = OnSurface;
						this.id = k
						break;
					end
				end
			end
		end
		*/

		/*
		for each photon along path do
			boolean mask = 0;
			int id = -1;
			for k,v in geometries do
				location = v[1]
				collision = photon-location
				dimension = v[2]
				OnSurface = location
				new_loc = collision + rd*jt
				distance = length(max(abs(new_loc)-dimension,vec3(0)))
				if(distance<E)then
					mask = 1
					id = k
					break					
				end
			end
			if mask = 1 then break
		end
		*/


		/*
		Ldist = nil
		OnSurface = nil
		for each photon along path do
			kdist = MAX
			kloc = nil
			for k,v in geometries do
				object = v
				collision = photon - object
				new_loc = collision + rd*jt
				dist = length(new_loc) - R
				if(dist<kdist)then
					kdist = dist
					kloc = new_loc
				end
			end
			if(kdist<E)then
				Ldist = kdist
				OnSurface = kloc
			end
		end
		*/

void main()
{	
	//向量并非绝对的，任何向量都可以用两个点的相对位置衡量
	//因此向量的模长指的便是两点间的距离
	//若依某个方程式vec3 G(F(x))得出的向量距离某一固定点始终为R的距离，则称该向量为球面
	//x是空间中任一点，F(x)为点X到固定点的距离，可能小于R，可能等于R，也可能大于R，经过筛选后只保留等于R的部分，该筛选称为函数G
	
	//路径追踪分为两个模型，一个基本理念
	//基本理念：从摄像机出发追踪场景
	//正交模型：按像素遍历光源起点（多个起点，一个方向）
	//点光模型：按像素遍历光源射角（一个起点，多个方向）
	//点光模型中的射角向量通过标准化形成基于XYZ权重的全自由度
	//向量的任一Component若能够在其他两组件变化的时候，保证无论他们取何值，在标准化后该组件值不变，则称为排除一个自由度

	//对光线追踪的每一次迭代而言，光子的位置都可以用碰撞标签Collision=光子位置Photon-光追对象位置Object表示
	//当碰撞标签大于R时，意味着光子在SDF球外
	//当碰撞标签小于R时，意味着光子在SDF球内
	vec3[2] array_pos = vec3[2](
		vec3(0.0,	0.0,	0.0),
		vec3(0.3,	0.0,	-0.2)
	);
	vec3[2] array_size = vec3[2](
		vec3(0.1,	0.1,	0.1),
		vec3(0.1,	0.2,	0.1)
	);
	vec3 Photon = vec3(pos , -3);//光子的位置可以用光源位置表示，所有方向上行进的光子都从光源位置开始迭代
	vec3 RaymarchDirection = normalize(vec3(0,0,0.5));//射线行进的所有方向组成一个圆锥体，Z的符号决定锥体的方向，Z的大小决定锥体的角度（影响同样Z值时的截面面积；截面越小，同样的可视部分映射在NDC上越大，同样的NDC能够显示的场景越残缺）
	float Distance = 99999;//该值理应维持99999或者位于（0,0.001）之间的值，意味着在有碰撞时光子到表面的距离。0.001是容差，在该容差范围内的距离都模糊算为重合（即碰撞），可以用于绘制体积光晕，输出颜色1-a*distance，范围在(1-0.001*a,1)之间，通过a的使用允许容差大于1（若a=1，容差最大为1，因为负颜色值强制取0，任何大于1的容差输出的结果与边界值相同）
	float JumpTo = 0;//从光源起点出发的射线每一次沿方向推进时，表示在该方向上已经走了多少距离，以及下一次推进要走多少距离的系数。|||注意：计算光子碰撞条件需要建立光子到光追对象位置的向量，而射线推进的方向与该向量无关，该向量表示任一位置的光子到光追对象的直线距离，不保证光子会沿着该向量方向或负方向推进|||附录：因为我们知道，以光子当前位置建立一个半径为光子到光追对象表面的最短直线距离的圆（半径相减，得表面间距离），圆上任意一点（从任意方向发射射线）要么落在光追对象表面上，要么尚未抵达（若未曾撞上，则错过），因此可以“跳过”一大部分距离，直接从圆上出发继续
	float MAX_ITERATION = 60;
	float E = 0.001;//容差
	vec3 Normal;
	for(int i=0;i<MAX_ITERATION;i++){
		//在一次迭代里面同时算光子到两个几何体的距离，并且取行进距离的最小值，这意味着将按照深度结合这两个物体在视野中的重合部位，并且如果一个物体无法包裹某块空间，如果另一个物体有包裹，则取更小值（虚空意味着最大值）
		//但是，直接求min(Distance1,Distance2)也是可以的，任何比99999小的都是0~0.001，因此最后的输出要么是0，要么就是1.
		float minDist = 1;//[0,E]U1
		vec3 OnSurface = vec3(-1,-1,-1);
		for(int count = 0;count < 2;count ++){
			vec3 Object = array_pos[count];//对象中心点位置
			vec3 Collision = Photon - Object;
			vec3 new_loc = Collision + RaymarchDirection * JumpTo;
			vec3 VDist3 = abs(new_loc) - array_size[count];
			VDist3 = max(VDist3,vec3(0));
			float dist = length(VDist3);
			if(dist < minDist){
				minDist = dist;
				vec3 mapped = new_loc;
				if(abs(new_loc.x)-array_size[count].x>-E){mapped.x = 0;}
				if(abs(new_loc.y)-array_size[count].y>-E){mapped.y = 0;}
				if(abs(new_loc.z)-array_size[count].z>-E){mapped.z = 0;}
				OnSurface = new_loc - mapped;
			}
		}
		if (minDist < E) {
			Distance = minDist;
			Normal = OnSurface;
			break;
		}
		
		JumpTo = JumpTo + minDist;
	}
	Normal = normalize(Normal);
	vec3 LightDirection = normalize(vec3(-0.6,0,-1));
	vec3 Luminance = vec3(dot(LightDirection,Normal));
	Luminance = floor(Luminance*20)/20;
	float lerp = clamp(1-Distance,0,1);
	vec3 returnValue = mix(vec3(1,0,0),Luminance,lerp);
	Colors = vec4(returnValue,1.0);
}