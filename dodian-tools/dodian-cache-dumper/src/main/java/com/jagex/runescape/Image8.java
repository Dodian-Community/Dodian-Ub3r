package com.jagex.runescape;

import com.displee.cache.index.archive.Archive;
import com.displee.cache.index.archive.file.File;

import java.awt.image.BufferedImage;
import java.io.IOException;

@SuppressWarnings("unused")
public class Image8 {

    public final int[] palette;
    public byte[] pixels;
    public int width;
    public int height;
    public int cropX;
    public int cropY;
    public int cropW;
    public int cropH;

    public Image8(Archive archive, String name, int index) throws IOException {
        File fileDat = archive.file(name + ".dat");
        File fileIdx = archive.file("index.dat");
        if (fileDat == null || fileIdx == null)
            throw new RuntimeException("Failed to find file...");

        Buffer dat = new Buffer(fileDat.getData());
        Buffer idx = new Buffer(fileIdx.getData());
        idx.position = dat.readUnsignedShort();
        cropW = idx.readUnsignedShort();
        cropH = idx.readUnsignedShort();
        int paletteSize = idx.readUnsignedByte();
        palette = new int[paletteSize];
        for (int i = 0; i < (paletteSize - 1); i++) {
            palette[i + 1] = idx.read24();
        }
        for (int i = 0; i < index; i++) {
            idx.position += 2;
            dat.position += idx.readUnsignedShort() * idx.readUnsignedShort();
            idx.position++;
        }
        cropX = idx.readUnsignedByte();
        cropY = idx.readUnsignedByte();
        width = idx.readUnsignedShort();
        height = idx.readUnsignedShort();
        int pixelOrder = idx.readUnsignedByte();
        int pixelCount = width * height;
        pixels = new byte[pixelCount];

        if (pixelOrder == 0) {
            for (int i = 0; i < pixelCount; i++) {
                pixels[i] = dat.readByte();
            }
            return;
        }

        if (pixelOrder == 1) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    pixels[x + (y * width)] = dat.readByte();
                }
            }
        }
    }

    public void shrink() {
        cropW /= 2;
        cropH /= 2;
        byte[] pixels = new byte[cropW * cropH];
        int off = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[((x + cropX) >> 1) + (((y + cropY) >> 1) * cropW)] = this.pixels[off++];
            }
        }
        this.pixels = pixels;
        width = cropW;
        height = cropH;
        cropX = 0;
        cropY = 0;
    }

    public void crop() {
        if ((width == cropW) && (height == cropH)) {
            return;
        }
        byte[] pixels = new byte[cropW * cropH];
        int off = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[x + cropX + ((y + cropY) * cropW)] = this.pixels[off++];
            }
        }
        this.pixels = pixels;
        width = cropW;
        height = cropH;
        cropX = 0;
        cropY = 0;
    }

    public void flipHorizontally() {
        byte[] pixels = new byte[width * height];
        int off = 0;
        for (int y = 0; y < height; y++) {
            for (int x = width - 1; x >= 0; x--) {
                pixels[off++] = this.pixels[x + (y * width)];
            }
        }
        this.pixels = pixels;
        cropX = cropW - width - cropX;
    }

    public void flipVertically() {
        byte[] pixels = new byte[width * height];
        int i = 0;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                pixels[i++] = this.pixels[x + (y * width)];
            }
        }
        this.pixels = pixels;
        cropY = cropH - height - cropY;
    }

    public void translate(int r, int g, int b) {
        for (int i = 0; i < palette.length; i++) {
            int red = (palette[i] >> 16) & 0xff;
            red += r;
            if (red < 0) {
                red = 0;
            } else if (red > 255) {
                red = 255;
            }
            int green = (palette[i] >> 8) & 0xff;
            green += g;
            if (green < 0) {
                green = 0;
            } else if (green > 255) {
                green = 255;
            }
            int blue = palette[i] & 0xff;
            blue += b;
            if (blue < 0) {
                blue = 0;
            } else if (blue > 255) {
                blue = 255;
            }
            palette[i] = (red << 16) + (green << 8) + blue;
        }
    }

    public void blit(int x, int y) {
        x += cropX;
        y += cropY;
        int dstOff = x + (y * Draw2D.width);
        int srcOff = 0;
        int height = this.height;
        int width = this.width;
        int dstStep = Draw2D.width - width;
        int srcStep = 0;
        if (y < Draw2D.top) {
            int trim = Draw2D.top - y;
            height -= trim;
            y = Draw2D.top;
            srcOff += trim * width;
            dstOff += trim * Draw2D.width;
        }
        if ((y + height) > Draw2D.bottom) {
            height -= (y + height) - Draw2D.bottom;
        }
        if (x < Draw2D.left) {
            int trim = Draw2D.left - x;
            width -= trim;
            x = Draw2D.left;
            srcOff += trim;
            dstOff += trim;
            srcStep += trim;
            dstStep += trim;
        }
        if ((x + width) > Draw2D.right) {
            int trim = (x + width) - Draw2D.right;
            width -= trim;
            srcStep += trim;
            dstStep += trim;
        }
        if ((width > 0) && (height > 0)) {
            blit(height, Draw2D.pixels, pixels, dstStep, dstOff, width, srcOff, palette, srcStep);
        }
    }

    public void blit(int height, int[] dst, byte[] src, int dstStep, int dstOff, int width, int srcOff, int[] palette, int srcStep) {
        int quarterWidth = -(width >> 2);
        width = -(width & 3);
        for (int y = -height; y < 0; y++) {
            for (int w = quarterWidth; w < 0; w++) {
                byte paletteIndex = src[srcOff++];

                if (paletteIndex != 0) {
                    dst[dstOff++] = palette[paletteIndex & 0xff];
                } else {
                    dstOff++;
                }

                paletteIndex = src[srcOff++];
                if (paletteIndex != 0) {
                    dst[dstOff++] = palette[paletteIndex & 0xff];
                } else {
                    dstOff++;
                }
                paletteIndex = src[srcOff++];
                if (paletteIndex != 0) {
                    dst[dstOff++] = palette[paletteIndex & 0xff];
                } else {
                    dstOff++;
                }
                paletteIndex = src[srcOff++];
                if (paletteIndex != 0) {
                    dst[dstOff++] = palette[paletteIndex & 0xff];
                } else {
                    dstOff++;
                }
            }
            for (int i = width; i < 0; i++) {
                byte paletteIndex = src[srcOff++];
                if (paletteIndex != 0) {
                    dst[dstOff++] = palette[paletteIndex & 0xff];
                } else {
                    dstOff++;
                }
            }
            dstOff += dstStep;
            srcOff += srcStep;
        }
    }

    public BufferedImage toBufferedImage() {
        int[] transPixels = new int[pixels.length];
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] != 0) {
                transPixels[i] = pixels[i] | 0xff000000;
            }
        }

        img.setRGB(0, 0, width, height, transPixels, 0, width);

        return img;
    }
}
