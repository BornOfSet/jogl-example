package jogl3;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL3;

import glm_.mat4x4.Mat4;

public class Utils {
	public static FloatBuffer GetBuffer4x4(Mat4 matrix) {
		FloatBuffer fb = FloatBuffer.allocate(16);
		fb.put(matrix.v00());fb.put(matrix.v01());fb.put(matrix.v02());fb.put(matrix.v03());
		fb.put(matrix.v10());fb.put(matrix.v11());fb.put(matrix.v12());fb.put(matrix.v13());
		fb.put(matrix.v20());fb.put(matrix.v21());fb.put(matrix.v22());fb.put(matrix.v23());
		fb.put(matrix.v30());fb.put(matrix.v31());fb.put(matrix.v32());fb.put(matrix.v33());
		fb.flip();
		return fb;
	}
	public static void PrintMatrix4x4(Mat4 matrix) {
		System.out.print(matrix.v00().toString() +"  "+ matrix.v01().toString() +"  "+ matrix.v02().toString() +"  "+ matrix.v03().toString()+"\n");
		System.out.print(matrix.v10().toString() +"  "+ matrix.v11().toString() +"  "+ matrix.v12().toString() +"  "+ matrix.v13().toString()+"\n");
		System.out.print(matrix.v20().toString() +"  "+ matrix.v21().toString() +"  "+ matrix.v22().toString() +"  "+ matrix.v23().toString()+"\n");
		System.out.print(matrix.v30().toString() +"  "+ matrix.v31().toString() +"  "+ matrix.v32().toString() +"  "+ matrix.v33().toString()+"\n");
	}
	public static void getErrors(GL3 gl,String additional) {
		int x;
		while(( x = gl.glGetError()) != GL3.GL_NO_ERROR) {
			System.out.println(x + "  " + additional);
		}
		System.out.println("Stack Cleared: " + x + "   " + additional + " Is Finished \n");
	}
	public static void getErrorsMultipleLines(GL3 gl,String additional) {
		int x;
		while(( x = gl.glGetError()) != GL3.GL_NO_ERROR) {
			System.out.println(x + "  " + additional);
		}
		System.out.println("Stack Cleared: " + x + "   " + additional + " Is Finished");
	}
}
