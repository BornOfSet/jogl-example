package jogl3.Trace;

import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class ShaderLoader {
	
	public static String LoadShader(String  path) throws IOException {
		Scanner scan= new Scanner(new FileReader(path));
		String str = "";
		try(scan) { //Auto close
			while(scan.hasNextLine()) {
				str = str  + scan.nextLine()+"\n";
			}
		}
		//文档中的换行在输出的时候不会自动换行
		//字符串并不需要附加引号
		//GLSL需要换行
		return str;
	}
}
