package jogl3.Advanced;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;

import jogl3.Controller;
import jogl3.Utils;

public class RTTManager {
	
	Controller c;
	IntBuffer RenderTarget = IntBuffer.allocate(1);
	IntBuffer Texture = IntBuffer.allocate(1);
	int TargetFrameSize;
	
	private boolean IsRenderingToTarget = true;
	
	public RTTManager(GLAutoDrawable Drawable , Controller c) {
		this.c = c;
		TargetFrameSize = c.S_map.getSquareSize();
		
		GL3 gl = Drawable.getGL().getGL3();
		Utils.getErrors(gl,"Setup");
		gl.glGenFramebuffers(1, RenderTarget);

		//■ ■ ■ ■ ■ ■ ■ ■ 
		//Named frame buffer operations are limited to only GL4 . That's why we have to set current buffer here. 
		//The following commands ( if they have things to do with FBO) are all performed on the CURRENT one.
		//What is point for you to glCheckFramebufferStatus here ? You haven't even completed all setups !  That's of course you got GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT  for this establishing fbo and no error for the established default buffer
		RenderToTarget(Drawable);
		//■ ■ ■ ■ ■ ■ ■ ■ 
		
		gl.glGenTextures(1, Texture);
		gl.glBindTexture(GL3.GL_TEXTURE_2D, Texture.get(0));
		gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGB, TargetFrameSize, TargetFrameSize, 0, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
		
		//■ ■ ■ ■ ■ ■ ■ ■ 
		//Here we will use the Texture as the current framebuffer's ( bind it with RenderToTarget!) GL_COLOR_ATTACHMENT0 channel 
		//This is basically restricting what GL_COLOR_ATTACHMENT0 looks like
		//From now on glCheckFramebufferStatus should return GL3.GL_FRAMEBUFFER_COMPLETE.
		//COMPLETE == There must be at least one color attachment assigned with a texture(composed of images of MIPMAP) to receive frag output at level n
		gl.glFramebufferTexture(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, Texture.get(0), 0);
		//■ ■ ■ ■ ■ ■ ■ ■ 
		
		IntBuffer Control = IntBuffer.allocate(2);
		Control.put(GL3.GL_NONE);
		Control.put(GL3.GL_COLOR_ATTACHMENT0);
		Control.flip();
		
		//■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ 
		//The mapping between fragment colors and actual buffers within a Framebuffer is defined by glDrawBuffers, which is part of the framebuffer's state
		//glDrawBuffers controls how FRAGMENT SHADER actually output colors into BUFFERS that is outside shading programs.
		//THERE'RE LINKS ALONG THE TRAVERSAL OF Control POINTING TO OPEN PORTALS AVAILABLE IN FRAGMENT SHADER
		//This is about WHEN GL_COLOR_ATTACHMENT0 is filled ( WHEN you're writing things into the GL_FRAMEBUFFER )
		//And WHEN GL_COLOR_ATTACHMENT0 is EMPTY ( WHEN you cleansed the FBO)
		//You have to bind GL_COLOR_ATTACHMENT0 the user-defined FBO so access to GL_COLOR_ATTACHMENT0 won't result in blackness if you're not drawing into the default buffer( you don't bind it to GL_COLOR_ATTACHMENT0 then it is bound to the default one)
		gl.glDrawBuffers(2, Control);
		//■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ ■ 
		
		gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
		Utils.getErrors(gl,"When setting up RTTManager       FrameBuffer Status Code:" + gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER));
	}
	
	public void RenderToTarget(GLAutoDrawable Drawable) {
		GL3 gl = Drawable.getGL().getGL3();
		//TODO------------Use Different Shaders------------TODO//
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, RenderTarget.get(0));	
		c.S_map.glResize(Drawable);
	}
	
	public void RenderToScreen(GLAutoDrawable Drawable) {
		GL3 gl = Drawable.getGL().getGL3();
		gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);	
		c.S_canvas.glResize(Drawable);//KEEP THE SIZE CONSISTENT OTHERWISE YOUR SCREEN IS CUT !
		//This thing needs more configuration 
		//Needs further screen-dividing support
		//WHEN YOU RE USING DUAL BUFFERS , THERE"RE ACTUALLY 3 BUFFERS IN SCENE
		//There's only one logical layer . So all changes are synchronized
	}
	
	public void ChangeState(GLAutoDrawable Drawable) {
		this.IsRenderingToTarget = !IsRenderingToTarget;
		if(IsRenderingToTarget) {
			RenderToTarget(Drawable);
			System.out.print("      ||  Swap to Target ||    ");
		}else {
			RenderToScreen(Drawable);
			System.out.print("      ||  Swap to Screen ||    ");
		}
	}
	
	public boolean Export(GLAutoDrawable Drawable) {
		System.out.print("  ||  IsRenderingToTarget:    "+IsRenderingToTarget + "");
		if(!IsRenderingToTarget) {
			System.out.print("  ||  Cancel  ||  ");
			return false;}
		GL3 gl = Drawable.getGL().getGL3();
		//The front is always front even if we're rendering into the attachment
		//The reason for a front to be black , is that we cleansed the front before we're going into the attachment
		//And because we don't update the front any more . We got a blackness when we're reading the front buffer
		//There is no GL_COLOR_ATTACHMENT0 in the default buffer ( which one is called FRONT)
		//We need to ensure no such error code (GL_INVALID_OPERATION ) will be generated
		gl.glReadBuffer(GL3.GL_COLOR_ATTACHMENT0);
		//8-8-8
		ByteBuffer data = ByteBuffer.allocate(TargetFrameSize*TargetFrameSize*3);//If you're going to use RGBA then it's 4 because you have each channel represented by one Byte , a pixel represented by 3/4 channels
		gl.glReadPixels(0, 0, TargetFrameSize, TargetFrameSize, GL3.GL_RGB, GL3.GL_UNSIGNED_BYTE, data);
		BufferedImage ImageBuffer = new BufferedImage(TargetFrameSize, TargetFrameSize, BufferedImage.TYPE_INT_RGB);
		WritableRaster ImageRaster = ImageBuffer.getRaster();
		for(int height = TargetFrameSize-1;height >= 0; height--) {//Otherwise it is upside down . You should not have height = TargetFrameSize . There's no such a index in Raster
			for(int width = 0;width < TargetFrameSize; width++) {
				byte r = data.get();
				byte g = data.get();
				byte b = data.get();
				//Byte自动提升(promotion)为int，按位与移除(mask off)补码和符号
				int[] pixel = {r&0xFF,g&0xFF,b&0xFF};
				ImageRaster.setPixel(width, height, pixel);
			}
		}
		try {
			ImageIO.write(ImageBuffer,"png",new File("out.png"));
		}catch(IOException e) {
			e.printStackTrace(); //BMP has problem with TYPE_INT_RGB
		}
		System.out.print("  ||  Exporting  ||  ");
		return true;
		/**
		//我不会推荐这种方法，简直是异端邪说，你明明已经给出了图像数组，干嘛还要再去光栅化把坐标转换为索引写入颜色呢
		//在使用中，你会希望通过graphics.drawRect(w, height - h, 1, 1);做长和高的双重for循环，绘制一个特定的像素 
	    //Returns a {@code Graphics2D} object for rendering into the specified {@link BufferedImage}.
		Graphics ImageGraphics = ImageBuffer.getGraphics();
		*/
		
	}
	
	public boolean IsRenderingToTarget() {
		return this.IsRenderingToTarget;
	}

}
