package com.fsck.k9.helper;

public class BitmapSize {
  
    public int width;
    public int height;
  
    public BitmapSize(int w, int h) {
        width = w;
        height = h;
    }
  
    public BitmapSize(String toString) {
      int index = toString.indexOf('x');
      width = Integer.parseInt(toString.substring(0,index));
      height = Integer.parseInt(toString.substring(index+1));
    }
    
    public int getSmallSide() {
        return Math.min(width,height);
    }
  
    public int getLargeSide() {
        return Math.max(width,height);
    }
  
    @Override
    public String toString() {
        return width + "x" + height;
    }
  
    /**
     * @param bitmapSize Bitmap-size to use as reference; this object will be checked against this parameters to see if it can fit inside or not.
     * @return True if this bitmap-size can fit inside the bitmap-size passes as argument; returns true also if it can fit after a 90° rotation
     */
    public boolean includedIn(BitmapSize bitmapSize) {
        if (getSmallSide() > bitmapSize.getSmallSide() || getLargeSide() > bitmapSize.getLargeSide()) return false;
        return true;
    }
  
    public boolean isPortrait() {
        return height>=width;
    }
  
    public boolean isSameOrientationThan(BitmapSize bitmapSize) {
        return !(isPortrait() ^ bitmapSize.isPortrait()); 
    }
}
