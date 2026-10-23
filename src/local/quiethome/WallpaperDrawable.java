package local.quiethome;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** A center-cropped, static local wallpaper. The shader is also shared with the glass lenses. */
final class WallpaperDrawable extends Drawable {
    final BitmapShader image;
    final float darkness;
    private final Bitmap bitmap;
    private final Paint photo=new Paint(3),shade=new Paint(3),bottom=new Paint(3);
    private final Matrix matrix=new Matrix();
    WallpaperDrawable(Bitmap bitmap,float darkness){
        this.bitmap=bitmap;this.darkness=darkness;
        image=new BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
        photo.setShader(image);shade.setColor(Color.argb(Math.round(darkness*255),0,0,0));
    }
    @Override protected void onBoundsChange(Rect r){
        float s=Math.max(r.width()/(float)bitmap.getWidth(),r.height()/(float)bitmap.getHeight());
        matrix.setScale(s,s);matrix.postTranslate((r.width()-bitmap.getWidth()*s)/2,(r.height()-bitmap.getHeight()*s)/2);
        image.setLocalMatrix(matrix);
        bottom.setShader(new LinearGradient(0,r.height()*.5f,0,r.height(),0x00000000,0x33000000,Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas canvas){canvas.drawRect(getBounds(),photo);canvas.drawRect(getBounds(),shade);canvas.drawRect(getBounds(),bottom);}
    @Override public void setAlpha(int a){photo.setAlpha(a);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){photo.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
