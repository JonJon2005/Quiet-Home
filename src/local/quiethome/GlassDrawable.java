package local.quiethome;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Analytic glass lens over the launcher's own gradient, without screen capture or blur. */
final class GlassDrawable extends Drawable {
    private final RuntimeShader shader;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final View owner;
    private final int[] position=new int[2];
    private final float radius;
    private float focus;
    private ValueAnimator animation;
    private final RectF bounds=new RectF();
    GlassDrawable(View owner,int[] background,int tint,boolean light,float density,WallpaperDrawable wallpaper) {
        this.owner=owner;radius=19*density;
        shader=new RuntimeShader(SOURCE);
        shader.setColorUniform("c0",background[0]);
        shader.setColorUniform("c1",background[background.length==3?1:0]);
        shader.setColorUniform("c2",background[background.length-1]);
        shader.setColorUniform("tint",tint);
        shader.setFloatUniform("lightMode",light?1f:0f);
        shader.setFloatUniform("imageOn",wallpaper==null?0f:1f);
        shader.setFloatUniform("darkness",wallpaper==null?0f:wallpaper.darkness);
        shader.setInputShader("wallpaper",wallpaper==null?
            new LinearGradient(0,0,1,1,background[0],background[background.length-1],Shader.TileMode.CLAMP):wallpaper.image);
        paint.setShader(shader);
    }
    void focus(boolean selected) {
        if(animation!=null)animation.cancel();
        animation=ValueAnimator.ofFloat(focus,selected?1f:0f);
        animation.setDuration(100);
        animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animation.addUpdateListener(a->{focus=(Float)a.getAnimatedValue();invalidateSelf();});
        animation.start();
    }
    @Override protected void onBoundsChange(Rect r) { bounds.set(r); }
    @Override public void draw(Canvas canvas) {
        if(bounds.isEmpty())return;
        owner.getLocationOnScreen(position);
        View root=owner.getRootView();
        shader.setFloatUniform("size",bounds.width(),bounds.height());
        shader.setFloatUniform("origin",position[0],position[1]);
        shader.setFloatUniform("screen",Math.max(1,root.getWidth()),Math.max(1,root.getHeight()));
        shader.setFloatUniform("radius",Math.min(radius,bounds.height()/2));
        shader.setFloatUniform("focus",focus);
        canvas.drawRoundRect(bounds,Math.min(radius,bounds.height()/2),Math.min(radius,bounds.height()/2),paint);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha);invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) {paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    private static final String SOURCE=String.join("\n",
        "uniform float2 size; uniform float2 origin; uniform float2 screen;",
        "uniform float radius; uniform float focus; uniform float lightMode;",
        "uniform shader wallpaper; uniform float imageOn; uniform float darkness;",
        "layout(color) uniform float4 c0; layout(color) uniform float4 c1;",
        "layout(color) uniform float4 c2; layout(color) uniform float4 tint;",
        "float3 scene(float2 p) {",
        " if(imageOn>0.5){float bottom=clamp((p.y/screen.y-0.5)*2.0,0.0,1.0);return wallpaper.eval(p).rgb*(1.0-darkness)*(1.0-bottom*0.2);}",
        " float t=clamp((p.x/screen.x+p.y/screen.y)*0.5,0.0,1.0);",
        " return mix(mix(c0.rgb,c1.rgb,clamp(t*2.0,0.0,1.0)),c2.rgb,clamp(t*2.0-1.0,0.0,1.0));",
        "}",
        "half4 main(float2 p) {",
        " float2 center=size*0.5; float2 v=p-center;",
        " float2 q=abs(v)-(center-float2(radius));",
        " float2 outer=max(q,float2(0.0));",
        " float sd=length(outer)+min(max(q.x,q.y),0.0)-radius;",
        " float2 n;",
        " if(length(outer)>0.001) n=normalize(outer)*sign(v);",
        " else n=q.x>q.y?float2(sign(v.x),0.0):float2(0.0,sign(v.y));",
        " float depth=max(0.0,-sd);",
        " float bevel=exp(-depth/11.0);",
        " float2 lens=v/max(center,float2(1.0));",
        " float2 refract=p+origin-n*bevel*(18.0+focus*8.0)-lens*(5.0+focus*4.0);",
        " float split=bevel*(1.3+focus);",
        " float3 base=scene(refract);",
        " if(bevel>0.2){base.r=scene(refract+n*split).r;base.b=scene(refract-n*split).b;}",
        " base=mix(base,tint.rgb,lightMode>0.5?0.20:0.13);",
        " float fresnel=pow(clamp(bevel,0.0,1.0),0.55);",
        " float facing=dot(n,normalize(float2(-0.55,-0.83)));",
        " float rim=exp(-abs(depth-1.2)/1.2);",
        " float caustic=exp(-abs(depth-5.0)/2.6);",
        " float directional=pow(max(facing,0.0),3.0);",
        " float under=pow(max(-facing,0.0),4.0);",
        " float y=p.y/max(size.y,1.0);",
        " float reflection=exp(-pow((y-(0.12+focus*0.07))/0.13,2.0));",
        " float3 sheen=float3(0.82,0.92,1.0);",
        " base+=sheen*reflection*(0.040+focus*0.035);",
        " base+=sheen*fresnel*(0.035+focus*0.035);",
        " base+=rim*(0.10+directional*0.34+focus*0.16)*(1.0-lightMode*0.55);",
        " base+=float3(0.56,0.74,1.0)*caustic*under*(0.12+focus*0.09);",
        " base-=float3(0.05)*caustic*(1.0-directional)*(1.0-lightMode*0.5);",
        " return half4(clamp(base,0.0,1.0),1.0);",
        "}");
}
