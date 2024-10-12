package jogl3;

public class Shaders {
	//If there is only a single output [in the fragment shader], the location does not need to be specified, in which case it defaults to zero.
	//If you don't do any explicit assignments, implementations usually will assign one of your output variables to location 0. 
	//

	public static final String[] VS_UV= {
			"#version 330 core\n",
			"layout (location = 0) in vec3 aPos;\n",
			"layout (location = 1) in vec3 aNormal;\n",
			"layout (location = 2) in vec3 aUV;\n",
			"uniform mat4 view;\n",
			"uniform mat4 scale;\n",
			"out vec3 Normal;\n",
			"void main()\n",
			"{\n",
			"   gl_Position = vec4(aUV.x*2-1, aUV.y*2-1, 0, 1.0);\n",
			"   Normal = aNormal;\n",
			"}\n" 	
		};
		
		//location 0 is the default screen-output point .
		//You have only one attachment in FBO that's why if Colors is not specified with layout location it's never used
		//In this case you want to draw and render different things to different channels
		//Or we can simply set different read and write buffer..... no , impossible
		//There is only one attachment in FBO . You can't have other things other than rt for a single attachment
		public static final String[] PS_UV= {
			"#version 330 core\n",
			"in vec3 Normal;\n",
			"layout(location = 0) out vec4 Colors;\n",
			"layout(location = 1) out vec3 rt;\n",
			"uniform vec3 light;\n",
			"void main()\n",
			"{\n",
			"   Colors = vec4(1.0 , 1.0 , 1.0 , 1.0);\n",
			"	 float strength = abs(dot(normalize(vec3(0,0,1)), light));\n",
			"	 Colors = Colors * strength;\n",
			"	 rt = Normal/2 + 0.5;\n",
			"}\n"	
		};
		
		public static final String[] VS= {
				"#version 330 core\n",
				"layout (location = 0) in vec3 aPos;\n",
				"layout (location = 1) in vec3 aNormal;\n",
				"uniform mat4 view;\n",
				"uniform mat4 scale;\n",
				"out vec3 Normal;\n",
				"out vec3 Loc;\n",
				"void main()\n",
				"{\n",
				"   gl_Position = view * scale * vec4(aPos.x, aPos.y, aPos.z, 1.0);\n",
				"   Normal = vec3(view * vec4(aNormal,1));\n",
				"	 Loc = aPos;\n",
				"}\n" 	
			};
			

			public static final String[] PS= {
				"#version 330 core\n",
				"in vec3 Normal;\n",
				"in vec3 Loc;\n",
				"layout(location = 0) out vec4 Colors;\n",
				"layout(location = 1) out vec4 aColors;\n",
				"uniform vec3 light;\n",
				"void main()\n",
				"{\n",
				"   Colors = vec4(1.0 , 1.0 , 1.0 , 1.0);\n",
				"	 float Zstrength = Loc.x*Loc.x + Loc.y*Loc.y + Loc.z*Loc.z;\n",
				"	 Zstrength = Zstrength / 2;\n",
				"	 float strength = max(dot(normalize(Normal), light) / 2 + 0.5, 0.10);\n",
				"	 strength = strength * 1 + 0.0 * Zstrength;\n",
				"	 Colors = strength;\n",
				"	 aColors = Colors;\n",
				"}\n"	
			};
}
