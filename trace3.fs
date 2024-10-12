#version 330 core
out vec4 Colors;
in vec2 pos;
uniform samplerBuffer ourTexture; //该uniform初始值为0，访问Texture0.我们已经在Texture0中定义纹理缓冲。根据sampler后缀，它会访问Texture0的纹理缓冲，访问的实际上是((通过(传入floatbuffer的arraybuffer)填充的)(tbo索引指向的)不可见缓冲对象)
uniform int TBOlayout[64];//uniformiv无法输入数据给uniform float
uniform float minX;
uniform float maxX;
uniform float minY;
uniform float maxY;
uniform float minZ;
uniform float maxZ;
uniform mat4 view;

//几何体访问
//通过PASS0将高模的信息储存在两张纹理中
//纹理A：尺寸上1000*1000，能够储存1mil个vec3，是点的位置
//纹理B：尺寸上1000*1000，与纹理A储存顺序相同，储存1mil个vec3，是对应的法线
//加速结构
//整个模型在受光照射的时候，可以分成4个部分。对于每一条射线，首先要做的是确定与哪一部分相交，从而在该部分中寻找目标三角形
//整个模型细分为4份，每一份细分为4瓣，每一瓣细分为4块，每一块细分为4域
//在找到需要的域后，对域内的三角形进行遍历
//细分的方法：首先剔除背面，然后计算剩下模型的边界框，将整个框内分为4个方格，每个方格内部再次细分
//遍历每个三角形，相当于每次3份地遍历所有顶点（步进为3）
//对一束射线遍历全部三角形，目的并非寻找最近或者寻找相似，而是因为一束射线必然落在三角形的内部，你要找到这个三角形的顶点。
//可以通过光子到分别任意两点的向量的叉与三角面的法线（硬法线）的比较，来确定是否落在面上

//加速结构的构建不能在着色器中，因为只需要构建一遍就行了，而不是每次计算都重建一遍
//在一个FS中放出的射线能够找到4份中的任意一份，因为一个FS对应一个VS，要在FS中找到全部VS，就必须在VS中传入全部顶点
//TBOlayout：指示如果追踪到某个结构要从ourTexture的哪个位置开始取

vec3 GeoAccess(){
	//注意我们直接将其命名为hit，因为要传递这个返回值，必然是基于纹理坐标的。只有正确的纹理坐标才能传达正确的vec3 pos，因此可以说，我们对texture唯一可知的就是对屏幕每个像素进行遍历时，通过适当方法计算出来的uv，获取到的“部分网格”   Alpha value should be 1 according to the internal format
	vec4 hit = texelFetch(ourTexture,1);
	return vec3(hit);
}
//现在我们已经将模型分成64份，并且按照从1到64的顺序传入GPU，目前尚不知道能否正常访问
//下一步，需要将加速区块传入着色器。需要一个uniform buffer，并且在每次FS的运行中构建出64个矩形，对光线进行判断

//Bounding Box 
float CalculateCenter(float min,float max){
	return (max+min)/2;
}


void main()
{	
	//每一个子盒只需要两个数据：min和max（统称为bounding）
	//推算每个子盒的bounding
	/*
		for each photon along path  do
			for k,v in geometries do
				const vec3 min
				const vec3 max
				vec3 object = (min + max)/2
				vec3 WDH = max - min
			end
		end
	*/
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
	//光子的位置Photon可以用光源位置表示，所有方向上行进的光子都从光源位置开始迭代
	//光追对象的位置Object
	//每一次对射线进行迭代的时候，使用光子相对光追物体的位置Collision来表达射线新抵达何处
	//屏幕上的每个像素代表一个放射方向Emitting
	//射线行进的所有方向RaymarchDirection组成一个圆锥体，Z的符号决定锥体的方向，Z的大小决定锥体的角度（影响同样Z值时的截面面积；截面越小，同样的可视部分映射在NDC上越大，同样的NDC能够显示的场景越残缺）
	//Emitting_debug，可以输出以检查Emitting(pos)的取值范围（-1到1）
	//BreakCondition对代表放射方向的一个像素计算，光线在该方向上不断的、离散的延长(for控制），BC代表迭代退出时一共迭代了几次。通过BC的大小可以判断该光线是否没有碰到任何东西而直达虚空，从而将代表该方向的屏幕像素染黑，指示它没有追踪到目标
	//Distance该值理应维持99999或者位于（0,0.001）之间的值，意味着在有碰撞时光子到表面的距离。0.001是容差，在该容差范围内的距离都模糊算为重合（即碰撞），可以用于绘制体积光晕，输出颜色1-a*distance，范围在(1-0.001*a,1)之间，通过a的使用允许容差大于1（若a=1，容差最大为1，因为负颜色值强制取0，任何大于1的容差输出的结果与边界值相同）
	//从光源起点出发的射线每一次沿方向推进时，MAX_ITERATION表示在该方向上已经走了多少距离，以及下一次推进要走多少距离的系数。|||注意：计算光子碰撞条件需要建立光子到光追对象位置的向量，而射线推进的方向与该向量无关，该向量表示任一位置的光子到光追对象的直线距离，不保证光子会沿着该向量方向或负方向推进|||附录：因为我们知道，以光子当前位置建立一个半径为光子到光追对象表面的最短直线距离的圆（半径相减，得表面间距离），圆上任意一点（从任意方向发射射线）要么落在光追对象表面上，要么尚未抵达（若未曾撞上，则错过），因此可以“跳过”一大部分距离，直接从圆上出发继续

	vec3 Photon = vec3(pos,-1);//光子的位置可以用光源位置表示，所有方向上行进的光子都从光源位置开始迭代
	vec4 _A = view * vec4(0,0,1,1);
	vec3 RaymarchDirection = normalize(vec3(_A));//射线行进的所有方向组成一个圆锥体，Z的符号决定锥体的方向，Z的大小决定锥体的角度（影响同样Z值时的截面面积；截面越小，同样的可视部分映射在NDC上越大，同样的NDC能够显示的场景越残缺）
	float Distance = 99999;//该值理应维持99999或者位于（0,0.001）之间的值，意味着在有碰撞时光子到表面的距离。0.001是容差，在该容差范围内的距离都模糊算为重合（即碰撞），可以用于绘制体积光晕，输出颜色1-a*distance，范围在(1-0.001*a,1)之间，通过a的使用允许容差大于1（若a=1，容差最大为1，因为负颜色值强制取0，任何大于1的容差输出的结果与边界值相同）
	float JumpTo = 0;//从光源起点出发的射线每一次沿方向推进时，表示在该方向上已经走了多少距离，以及下一次推进要走多少距离的系数。|||注意：计算光子碰撞条件需要建立光子到光追对象位置的向量，而射线推进的方向与该向量无关，该向量表示任一位置的光子到光追对象的直线距离，不保证光子会沿着该向量方向或负方向推进|||附录：因为我们知道，以光子当前位置建立一个半径为光子到光追对象表面的最短直线距离的圆（半径相减，得表面间距离），圆上任意一点（从任意方向发射射线）要么落在光追对象表面上，要么尚未抵达（若未曾撞上，则错过），因此可以“跳过”一大部分距离，直接从圆上出发继续
	float MAX_ITERATION = 60;//考虑并行性，这个值太大，会造成性能浪费在虚空束上
	float E = 0.002;//容差
	vec3 Normal;
	int ID = -1;
	for(int i=0;i<MAX_ITERATION;i++){
		//在一次迭代里面同时算光子到两个几何体的距离，并且取行进距离的最小值，这意味着将按照深度结合这两个物体在视野中的重合部位，并且如果一个物体无法包裹某块空间，如果另一个物体有包裹，则取更小值（虚空意味着最大值）
		//但是，直接求min(Distance1,Distance2)也是可以的，任何比99999小的都是0~0.001，因此最后的输出要么是0，要么就是1.
		float minDist = Distance;//[0,E]U1
		vec3 OnSurface = vec3(-1,-1,-1);
		int id = 0;
		int id_return = -1;
		//几何体循环是否可以Break？我感觉外部if可以合并到内部if，如果dist<E，就连续打破三个循环退出，代表已经找到了符合条件的BOX
		//不能。你要找到最短距离，然后将该最短距离当做下一次追踪的步长。这个算法的实现在于for k,v in all dist do minDist = min(minDist,dist) endfor
		for(int _x = 0;_x < 4; _x++){
			id = _x * 16;
			for(int _y = 0;_y < 4; _y++){
				id = _x * 16 + _y * 4;
				for(int _z = 0;_z < 4; _z++){
					id = _x * 16 + _y * 4 + _z;
					vec3 BBmin = vec3(_X[_x],_Y[_y],_Z[_z]);//Bounding Box Left Limits
					vec3 BBmax = vec3(_X[_x+1],_Y[_y+1],_Z[_z+1]);
					vec3 Object = (BBmin + BBmax)/2;
					vec3 BoundingSize = BBmax - BBmin;
					vec3 Collision = Photon - Object;
					vec3 new_loc = Collision + RaymarchDirection * JumpTo;
					vec3 VDist3 = abs(new_loc) - BoundingSize/2;
					VDist3 = max(VDist3,vec3(0));
					float dist = length(VDist3);
					if(dist < minDist){
						minDist = dist;//minDist = min(minDist,dist)
						vec3 mapped = new_loc;
						if(abs(new_loc.x)-BoundingSize.x/2>-E){mapped.x = 0;}
						if(abs(new_loc.y)-BoundingSize.y/2>-E){mapped.y = 0;}
						if(abs(new_loc.z)-BoundingSize.z/2>-E){mapped.z = 0;}
						//这是法线的计算方式
						//new_loc基本上指的是，相当于物体的这个绿色的中心，表面上的点的方向，比如在光的路线上，每一光子相当于物体中心在何处
						//这样的结果其实是一个球
						//所以我们要让它变硬
						//这是正确的法线方向emmmm反正就是这样，这个normal可以通过向量的三角形公式算出来
						OnSurface = new_loc - mapped * 1;
						id_return = id;
					}
				}
			}
		}
		if (minDist < E) {
			Distance = minDist;
			Normal = OnSurface;
			ID = id_return;
			break;
		}
		
		JumpTo = JumpTo + minDist;
	}

	Normal = normalize(Normal);
	vec3 LightDirection = normalize(vec3(0.2,0.9,-1));
	vec3 Luminance = vec3(dot(LightDirection,Normal));
	Luminance = floor(Luminance*20)/20;
	float light_mask = clamp(1-Distance,0,1);
	vec3 light = mix(vec3(1,1,0),Luminance,light_mask);
	//我们还可以给他加个光


	//ID取值范围 : 0~63 (总共64个)
	int lerp = clamp(ID+1,0,1);


	//所有顶点位置都是vec3，vec3由3个float组成。假设总共max个float变量，换算为vec3，可读取max/3个顶点位置，映射世界空间中任一坐标
	//总共64个int指针，根据64个次级box中容纳的vertex position（vec3型变量）数量，依次从左往右指向每一组vertex position在ourTexture中的开始位置
	//texelFetch返回vec4型变量，依据tbo的构造格式，实际上有max个float变量参与构造，但是ourTexture的数量上限为max/3，不要超出下标
	//如何提取顶点位置（world position）信息
	int pointer = -1;
	int limit = -1;
	if (ID == 0) {
		pointer = 0;
		limit = TBOlayout[ID];
	}
	if (ID > 0) {
		pointer = TBOlayout[ID-1];
		limit = TBOlayout[ID];
	}
	vec3 returnValue = vec3(0,0,0);
	for(int x = pointer;x<limit;x++){
		vec3 geo_position = vec3(texelFetch(ourTexture,x));
		vec3 march_trace = geo_position - Photon;//行进方向
		vec3 judge = abs(cross(march_trace,RaymarchDirection));//是否平行，靠近0就是平行
		if(judge.x < E && judge.y < E && judge.z < E){
			returnValue = vec3(1,1,1);
			break;
		}
	}
	vec3 red = vec3(1,0,0) * lerp;
	Colors = vec4(red * 0 + returnValue * light,1);
}















	/*
	int pointer = -1;
	int limit = -1;
	if (ID == 0) {
		pointer = 0;
		limit = TBOlayout[ID];
	}
	if (ID > 0) {
		pointer = TBOlayout[ID-1];
		limit = TBOlayout[ID];
	}
	vec3 returnValue = vec3(0,0,0);
	int loop = limit - pointer;
	for(int x = 0;x < loop;x++){
		vec3 geo_position = vec3(texelFetch(ourTexture,pointer + x));
		//新落点=起点+方向*t
		//点P在线上：存在t，使点P=起点+方向*t
		vec3 t = (geo_position - Photon)/RaymarchDirection;
		float tx_ty = abs(t.x - t.y);
		float ty_tz = abs(t.y - t.z);
		if(tx_ty + ty_tz < 10){
			returnValue = vec3(1,1,1);
			break;
		}
	}
	//下标越界：会导致GPU超时，设备丢失
	float OutColor = ID + 0;//lmao . ID is int
	OutColor /= 64;*/


	/*
		vec3 geo_position = vec3(texelFetch(ourTexture,x));
		vec3 t = (geo_position - Photon)/RaymarchDirection;
		//草，分母为0，正视图是黑的
		float tx_ty = t.x / t.y;
		float ty_tz = t.y / t.z;
		if(abs(tx_ty - ty_tz) < E){
			returnValue = vec3(1);
			break;
		}
		float tx_ty = abs(t.x - t.y);
		float ty_tz = abs(t.y - t.z);
		if(tx_ty + ty_tz < 1){
			returnValue = vec3(1,1,1);
			break;
		}		
	*/