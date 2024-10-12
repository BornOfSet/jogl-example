#version 330 core
out vec4 Colors;
in vec2 pos;

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
	vec3 Photon = vec3(pos , -3);//光子的位置可以用光源位置表示，所有方向上行进的光子都从光源位置开始迭代
	vec3 SphereObject1 = vec3(0.3,0,0.0);//光追对象的位置
	vec3 SphereObject2 = vec3(0.0,0.0,0);
	vec3 Collision1 = Photon - SphereObject1;//每一次对射线进行迭代的时候，使用光子相对光追物体的位置来表达射线新抵达何处
	vec3 Collision2 = Photon - SphereObject2;
	vec2 Emitting = pos;//屏幕上的每个像素代表一个放射方向
	vec3 RaymarchDirection = normalize(vec3(0,0,0.5));//射线行进的所有方向组成一个圆锥体，Z的符号决定锥体的方向，Z的大小决定锥体的角度（影响同样Z值时的截面面积；截面越小，同样的可视部分映射在NDC上越大，同样的NDC能够显示的场景越残缺）
	vec4 Emitting_debug = vec4((pos+1)/2, 0.0 , 1.0);//一个颜色，可以输出以检查Emitting(pos)的取值范围（-1到1）
	int BreakCondition = 0;//对代表放射方向的一个像素计算，光线在该方向上不断的、离散的延长(for控制），BC代表迭代退出时一共迭代了几次。通过BC的大小可以判断该光线是否没有碰到任何东西而直达虚空，从而将代表该方向的屏幕像素染黑，指示它没有追踪到目标
	float Distance = 99999;//该值理应维持99999或者位于（0,0.001）之间的值，意味着在有碰撞时光子到表面的距离。0.001是容差，在该容差范围内的距离都模糊算为重合（即碰撞），可以用于绘制体积光晕，输出颜色1-a*distance，范围在(1-0.001*a,1)之间，通过a的使用允许容差大于1（若a=1，容差最大为1，因为负颜色值强制取0，任何大于1的容差输出的结果与边界值相同）
	float JumpTo = 0;//从光源起点出发的射线每一次沿方向推进时，表示在该方向上已经走了多少距离，以及下一次推进要走多少距离的系数。|||注意：计算光子碰撞条件需要建立光子到光追对象位置的向量，而射线推进的方向与该向量无关，该向量表示任一位置的光子到光追对象的直线距离，不保证光子会沿着该向量方向或负方向推进|||附录：因为我们知道，以光子当前位置建立一个半径为光子到光追对象表面的最短直线距离的圆（半径相减，得表面间距离），圆上任意一点（从任意方向发射射线）要么落在光追对象表面上，要么尚未抵达（若未曾撞上，则错过），因此可以“跳过”一大部分距离，直接从圆上出发继续
	float MAX_ITERATION = 60;
	float R1 = 0.5;//SDF半径
	float R2 = 0.3;//SDF半径
	float E = 0.01;//容差
	vec3 OnSurface1 = SphereObject1;//表面上一点
	vec3 OnSurface2 = SphereObject2;//表面上一点
	int id = -1;
	for(int i=0;i<MAX_ITERATION;i++){
		//在一次迭代里面同时算光子到两个几何体的距离，并且取行进距离的最小值，这意味着将按照深度结合这两个物体在视野中的重合部位，并且如果一个物体无法包裹某块空间，如果另一个物体有包裹，则取更小值（虚空意味着最大值）
		//但是，直接求min(Distance1,Distance2)也是可以的，任何比99999小的都是0~0.001，因此最后的输出要么是0，要么就是1.
		BreakCondition = i;
		vec3 new_loc1 = Collision1 + RaymarchDirection * JumpTo;
		vec3 new_loc2 = Collision2 + RaymarchDirection * JumpTo;
		float dist1 = length(new_loc1)  - R1;
		float dist2 = length(new_loc2)  - R2;
		float dist = min(dist1,dist2);
		if (dist < E) {
			Distance = dist;
			OnSurface1 = new_loc1;
			OnSurface2 = new_loc2;
			if(dist1<dist2){id = 1;}
			if(dist1>dist2){id = 2;}
			break;
		}
		
		JumpTo = JumpTo + dist;
	}
	vec3 Normal;
	if(id==1){Normal = OnSurface1;} //碰撞处几何表面的法线。注意，Normal并非onSurface-Object。在计算Collision的时候已经考虑到了对Object的相对方向，可以说OnSurface是从Object球心出发的，自然指的是法线
	if(id==2){Normal = OnSurface2;}
	Normal = normalize(Normal);
	vec3 LightDirection = normalize(vec3(-1,0,-1));
	vec3 Luminance = vec3(dot(LightDirection,Normal));
	Luminance = floor(Luminance*8)/8;
	float lerp = clamp(1-Distance,0,1);
	vec3 returnValue = mix(vec3(1,0,0),Luminance,lerp);
	Colors = vec4(returnValue,1.0);
}