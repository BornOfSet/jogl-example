#version 330 core
out vec4 Colors;
in vec2 pos;
uniform samplerBuffer ourTexture; //该uniform初始值为0，访问Texture0.我们已经在Texture0中定义纹理缓冲。根据sampler后缀，它会访问Texture0的纹理缓冲，访问的实际上是((通过(传入floatbuffer的arraybuffer)填充的)(tbo索引指向的)不可见缓冲对象)
uniform float TBOlayout[64];
uniform float minX;
uniform float maxX;
uniform float minY;
uniform float maxY;
uniform float minZ;
uniform float maxZ;
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
	vec4 hit = texelFetch(ourTexture,1);//注意我们直接将其命名为hit，因为要传递这个返回值，必然是基于纹理坐标的。只有正确的纹理坐标才能传达正确的vec3 pos，因此可以说，我们对texture唯一可知的就是对屏幕每个像素进行遍历时，通过适当方法计算出来的uv，获取到的“部分网格”   Alpha value should be 1 according to the internal format
	return vec3(hit);
}
//现在我们已经将模型分成64份，并且按照从1到64的顺序传入GPU，目前尚不知道能否正常访问
//下一步，需要将加速区块传入着色器。需要一个uniform buffer，并且在每次FS的运行中构建出64个矩形，对光线进行判断


void main()
{	
	//推算Bounding boxes
	float midX = (minX + maxX)/2;
	float midY = (minY + maxY)/2;
	float midZ = (minZ + maxZ)/2;
	float d4Xleft = (minX + midX)/2;
	float d4Xright = (maxX + midX)/2;
	float d4Yleft = (minY + midY)/2;
	float d4Yright = (maxY + midY)/2;
	float d4Zleft = (minZ + midZ)/2;
	float d4Zright = (maxZ + midZ)/2;
	/*for X
	  for Y
	  for Z
		//给出任意3d dimension，按照此算法，最终得到的结果是以0为中心，WHD各自为2倍dimension的BOX。如果对该BOX的各个顶点添加一个向量，则相当于在该方向上平移
		//对于平移后的矩形，中心的位置等于0+vec3。中心线（中心点）不在任何一个子BOX内部，而是在边缘上。要求子BOX的中心，你希望通过(minX+d4Xleft)/2或者(d4Xright+midX)/2得到。
		//这也是为什么我注释了vec3 Deposilize = abs(new_loc)-offset;//重置到原点坐标，移除位置影响，使他们成为纯粹方向向量
		//任何减在abs(p)上的，都是WDH的一部分。如果将它看做最后的镜像BOX结果，那么offset的增减只能影响以0为中心的BOX的WDH
		//如果要实现对BOX的位移，而非在某个轴向上的扩张，你需要停止使用镜像算法。
			
		//WHD(width height depth)代表的是从原点出发，对以原点为中心呈现三轴对称的长方体，因此效果上等同于对BOX八个角的方向向量表达。方向向量是从原点出发的不同方向的向量，彼此之间没有位置差别，但有长度的差别（不是单位向量）
		//长方体的检测算法，可以看做是圆球的扩张：需要对每个轴讨论，单独一个箱体需要分三个轴讨论，但除此之外没有改变，你依然要算被减数，从而推到距离场，只不过是对每个轴而言是否落在某个分界线之后/之前（max(distance,0)的作用）
		//vec3 PerSingleAxisDistance = Deposilize - dimension; //单轴上距离，该向量应该逐部件理解，其整体不具有意义
		//vec3 Vacuum = max(PerSingleAxisDistance , vec3(0)); //如果不加以约束，最终的结果只是对单独的一条直线x=?进行左右判定，但通过消灭所有负数化作0，能够仅仅保留右部判定
		//float Cling = length(Vacuum); //紧贴矩形边界框。假设每一个平面上都有一个空缺（Vacuum），在空缺之外的所有数据集都是连续的，唯独空缺内部的数据集是单一的，那么对整个流体空间做三维投影，所有空缺的交集处，就是BOX体积。
		//想象光线在流体空间中前进，当它靠近该箱体到一定距离的时候（由容差决定），颜色改变（实际就是靠近的程度，距离的多少），因此越是紧贴表面，根据靠近的距离数值lerp，就越是倾向于另一个颜色进行变化
		//Cling要做到越小，则要求在每个轴上都极度接近交界线。注意，该几何体具有体积，因为在几何体内部，数据总是为0
		//如何判断数学转折？数学转折指的是不存在真正的平面结构（无法计算法线），只存在于数学上的强硬转折。事实上，该算法没有采用逐通道检测的方式将对转折角的碰撞判断拆分为对三个方向上各自的判断并取与运算
	*/

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
	vec3 Photon = vec3(0 , 0 , -3);//光子的位置可以用光源位置表示，所有方向上行进的光子都从光源位置开始迭代
	vec3 Object = vec3(0,0,0);//光追对象的位置
	vec3 Collision = Photon - Object;//每一次对射线进行迭代的时候，使用光子相对光追物体的位置来表达射线新抵达何处
	vec2 Emitting = pos;//屏幕上的每个像素代表一个放射方向
	vec3 RaymarchDirection = normalize(vec3(Emitting,0.5));//射线行进的所有方向组成一个圆锥体，Z的符号决定锥体的方向，Z的大小决定锥体的角度（影响同样Z值时的截面面积；截面越小，同样的可视部分映射在NDC上越大，同样的NDC能够显示的场景越残缺）
	vec4 Emitting_debug = vec4((pos+1)/2, 0.0 , 1.0);//一个颜色，可以输出以检查Emitting(pos)的取值范围（-1到1）
	int BreakCondition = 0;//对代表放射方向的一个像素计算，光线在该方向上不断的、离散的延长(for控制），BC代表迭代退出时一共迭代了几次。通过BC的大小可以判断该光线是否没有碰到任何东西而直达虚空，从而将代表该方向的屏幕像素染黑，指示它没有追踪到目标
	float Distance = 99999;//该值理应维持99999或者位于（0,0.001）之间的值，意味着在有碰撞时光子到表面的距离。0.001是容差，在该容差范围内的距离都模糊算为重合（即碰撞），可以用于绘制体积光晕，输出颜色1-a*distance，范围在(1-0.001*a,1)之间，通过a的使用允许容差大于1（若a=1，容差最大为1，因为负颜色值强制取0，任何大于1的容差输出的结果与边界值相同）
	float JumpTo = 0;//从光源起点出发的射线每一次沿方向推进时，表示在该方向上已经走了多少距离，以及下一次推进要走多少距离的系数。|||注意：计算光子碰撞条件需要建立光子到光追对象位置的向量，而射线推进的方向与该向量无关，该向量表示任一位置的光子到光追对象的直线距离，不保证光子会沿着该向量方向或负方向推进|||附录：因为我们知道，以光子当前位置建立一个半径为光子到光追对象表面的最短直线距离的圆（半径相减，得表面间距离），圆上任意一点（从任意方向发射射线）要么落在光追对象表面上，要么尚未抵达（若未曾撞上，则错过），因此可以“跳过”一大部分距离，直接从圆上出发继续
	float MAX_ITERATION = 10;
	float R = 1.0;//SDF半径
	float E = 0.01;//容差
	for(int i=0;i<MAX_ITERATION;i++){
		BreakCondition = i;
		vec3 new_loc = Collision + RaymarchDirection * JumpTo;
		float dist = length(new_loc) - R;
		if (dist < E) {
			Distance = dist;
			break;
		} 
		JumpTo = JumpTo + dist;
	}
	float returnValue = clamp(1-Distance,0,1);
	Colors = vec4(returnValue,returnValue,returnValue,1.0);
}