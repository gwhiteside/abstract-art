package net.georgewhiteside.android.abstractart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.georgewhiteside.android.aapreset.Image;
import net.georgewhiteside.android.abstractart.RectanglePacker.Rectangle;

// super quick and dirty class, designed for a specific case, not well-tested, beware, etc.

public class TextureAtlas {
	
	private byte[] atlas;
	private int width;
	private int height;
	int size;
	
	RectanglePacker<Integer> packer;
	
	public TextureAtlas() {
		
	}
	
	public void generateAtlas(List<Image> images) {
		int requiredArea = 0;
		int maxWidth = 0;
		int maxHeight = 0;
		
		for(Image image : images) {
			int width = image.getWidth();
			int height = image.getHeight();
			
			requiredArea += width * height;
			
			if(width > maxWidth) {
				maxWidth = width;
			}
			
			if(height > maxHeight) {
				maxHeight = height;
			}
		}
		
		int outWidth = maxWidth;
		int outHeight = maxHeight;
		int area = outWidth * outHeight;
		
		while(area < requiredArea) {
			if(outWidth < outHeight) {
				outWidth *= 2;
			} else {
				outHeight *= 2;
			}
			area = outWidth * outHeight;
		}
		
		width = outWidth;
		height = outHeight;
		
		while(!checkpack(images, width, height)) {
			if(width < height) {
				width *= 2;
			} else {
				height *= 2;
			}
		}
		
		pack(images);
	}
	
	private void write(Image src, byte[] dst, Rectangle dstRect) {
		byte[] srcPixels = src.getRgbaBytes();
		int srcWidth = src.getWidth();
		int srcHeight = src.getHeight();
		int dstX = dstRect.x;
		int dstY = dstRect.y;
		
		for(int y = 0; y < srcHeight; y++) {
			for(int x = 0; x < srcWidth; x++) {
				atlas[(y + dstY) * width * 4 + (x + dstX) * 4 + 0] = srcPixels[y * srcWidth * 4 + x * 4 + 0];
				atlas[(y + dstY) * width * 4 + (x + dstX) * 4 + 1] = srcPixels[y * srcWidth * 4 + x * 4 + 1];
				atlas[(y + dstY) * width * 4 + (x + dstX) * 4 + 2] = srcPixels[y * srcWidth * 4 + x * 4 + 2];
				atlas[(y + dstY) * width * 4 + (x + dstX) * 4 + 3] = srcPixels[y * srcWidth * 4 + x * 4 + 3];
			}
		}
	}
	
	private void pack(List<Image> images) {
		atlas = new byte[width * height * 4];
		
		for(int i = 0; i < images.size(); i++) {
			Image image = images.get(i);
			Rectangle dstRect = packer.findRectangle(i);
			write(image, atlas, dstRect);
		}
		
		size = images.size();
	}
	
	private boolean checkpack(List<Image> images, int width, int height) {
		packer = new RectanglePacker<Integer>(width, height, 0);
		
		for(int i = 0; i < images.size(); i++) {
			Image image = images.get(i);
			//Node node = root.insert(image, i);
			
			Rectangle dstRect = packer.insert(image.getWidth(), image.getHeight(), i);
			
			if(dstRect == null) {
				return false;
			}
		}
		return true;
	}
	
	public byte[] getAtlas() {
		return atlas;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int size() {
		return size;
	}
}
