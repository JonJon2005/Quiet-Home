package local.quiethome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.tv.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Local-only TV home. Uses TCL's TV player for all video rendering. */
public class HomeActivity extends Activity {
    private TvInputManager inputs;
    private LinearLayout cards;
    private TextView status;
    private String focused = "";
    private final Handler handler = new Handler();
    private int white, muted, surface, inactive, border, toolFocus;
    private String theme;
    private boolean customizing=false, glass=false;
    private final String[] wallpaperNames={"Lake McDonald","McWay Falls","Great Sand Dunes"};
    private final String[] wallpaperFiles={"alpine","coast","dunes"};
    private final java.util.concurrent.ExecutorService wallpaperWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final android.util.LruCache<String,Bitmap> wallpaperCache=new android.util.LruCache<String,Bitmap>(48*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getAllocationByteCount();}
    };
    private String loadedWallpaper="",loadingWallpaper="";
    private Bitmap wallpaperBitmap;
    private WallpaperDrawable photoScene;
    private String wallpaperKey(){return getPreferences(0).getString("wallpaper","");}
    private boolean isLight(){return "light".equals(theme) && wallpaperKey().isEmpty();}
    private String backgroundLabel(){
        String key=wallpaperKey();for(int n=0;n<wallpaperFiles.length;n++)if(wallpaperFiles[n].equals(key))return wallpaperNames[n];
        return theme.equals("oled")?"Pure black":backgroundNames[backgroundIndex()];
    }
    private void loadWallpaper(){
        final String key=wallpaperKey();
        if(key.isEmpty() || key.equals(loadedWallpaper) || key.equals(loadingWallpaper))return;
        loadingWallpaper=key;
        wallpaperWorker.execute(()->{
            try{
                Bitmap bitmap=wallpaperCache.get(key);
                if(bitmap==null){
                    BitmapFactory.Options options=new BitmapFactory.Options();
                    int width=getResources().getDisplayMetrics().widthPixels;
                    options.inSampleSize=width<=1920?2:1;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
                    try(java.io.InputStream stream=getAssets().open("wallpapers/"+key+".jpg")){bitmap=BitmapFactory.decodeStream(stream,null,options);}
                    if(bitmap==null)throw new java.io.IOException("Wallpaper decode failed");
                    wallpaperCache.put(key,bitmap);
                }
                final Bitmap ready=bitmap;
                handler.post(()->{if(isDestroyed())return;loadingWallpaper="";if(!key.equals(wallpaperKey()))return;
                    wallpaperBitmap=ready;loadedWallpaper=key;if(customizing)customization();else buildHome();});
            }catch(Exception e){android.util.Log.e("QuietHome","Wallpaper load",e);handler.post(()->{
                if(isDestroyed())return;loadingWallpaper="";if(key.equals(wallpaperKey())){
                    getPreferences(0).edit().remove("wallpaper").apply();Toast.makeText(this,"Wallpaper unavailable. Using gradient.",0).show();
                    if(customizing)customization();else buildHome();}
            });}
        });
    }
    private String inputSignature="";
    private final Runnable refreshTask=()->refresh();
    private void scheduleRefresh(){handler.removeCallbacks(refreshTask);handler.postDelayed(refreshTask,60);}
    private Object backlight;
    private Class<?> backlightClass, applyClass, actClass;
    private final String[] backgroundNames={"Original midnight","Ocean blue","Aurora teal","Forest green","Sunset amber","Rose","Purple haze","Graphite"};
    private final int[][] darkGradients={
        {0xff172d47,0xff111827,0xff201b35}, {0xff153d65,0xff102740,0xff101928},
        {0xff124944,0xff12353d,0xff18233e}, {0xff264631,0xff172f2b,0xff121e25},
        {0xff5a3827,0xff392833,0xff201e32}, {0xff512c43,0xff35233b,0xff1b2134},
        {0xff402b62,0xff292343,0xff17283b}, {0xff363c45,0xff232931,0xff151a22}};
    private final int[][] lightGradients={
        {0xfff5f7fb,0xffe9eef6,0xffe9e4f2}, {0xffe6f3ff,0xffd7e8fa,0xffedf4fc},
        {0xffdcf5ed,0xffdceff4,0xffe5e9f8}, {0xffe7f1d9,0xffdcece1,0xffe5eff0},
        {0xffffedd4,0xfff7dfd8,0xffeee3f2}, {0xffffe4ec,0xfff1dfee,0xffe7e7f7},
        {0xffeee2ff,0xffe7e1f5,0xffdfeafa}, {0xfff3f4f5,0xffe1e5e9,0xffedf0f3}};
    private int backgroundIndex(){return Math.max(0,Math.min(backgroundNames.length-1,getPreferences(0).getInt("background",0)));}
    private final TvInputManager.TvInputCallback callback = new TvInputManager.TvInputCallback() {
        @Override public void onInputStateChanged(String id, int state) { scheduleRefresh(); }
        @Override public void onInputAdded(String id) { scheduleRefresh(); }
        @Override public void onInputRemoved(String id) { scheduleRefresh(); }
        @Override public void onInputUpdated(String id) { scheduleRefresh(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        inputs = (TvInputManager)getSystemService(TV_INPUT_SERVICE);
        buildHome();
    }
    private void palette() {
        theme=getPreferences(0).getString("theme","dark");
        glass=getPreferences(0).getBoolean("liquid_glass",false) && Build.VERSION.SDK_INT>=33;
        boolean light=isLight();
        white=light?0xff182235:0xfff5f7ff; muted=light?0xff526077:0xff9ba6be;
        if(!wallpaperKey().isEmpty())muted=0xffd6deec;
        surface=light?0xffe2e8f1:theme.equals("oled")?0xff111111:0xff29364a;
        inactive=light?0xffe6e9ee:theme.equals("oled")?0xff090909:0xff252e3e;
        border=light?0xff3a6598:0xffe1ebff; toolFocus=light?0xffc8d9ed:0xff344762;
    }
    private LinearLayout page() {
        palette();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setClipChildren(false); root.setClipToPadding(false);
        root.setPadding(dp(52),dp(30),dp(52),dp(25));
        int[] bg=theme.equals("oled")?new int[]{0xff000000,0xff000000}:
            isLight()?lightGradients[backgroundIndex()]:darkGradients[backgroundIndex()];
        FrameLayout backdrop=new FrameLayout(this);
        photoScene=null;
        if(!wallpaperKey().isEmpty() && wallpaperKey().equals(loadedWallpaper) && wallpaperBitmap!=null){
            photoScene=new WallpaperDrawable(wallpaperBitmap,getPreferences(0).getInt("wallpaper_shade",30)/100f);
            backdrop.setBackground(photoScene);
        }else backdrop.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,bg));
        if(customizing){
            ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
            scroll.addView(root,new ScrollView.LayoutParams(-1,-2));backdrop.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        }else backdrop.addView(root,new FrameLayout.LayoutParams(-1,-1));
        setContentView(backdrop);loadWallpaper();
        return root;
    }
    private void buildHome() {
        customizing=false;
        inputSignature="";
        LinearLayout root=page();
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("HOME",14,muted); brand.setLetterSpacing(.22f);
        top.addView(brand,new LinearLayout.LayoutParams(0,dp(35),1));
        TextView settingsTool=compactTool("Settings",()->settings());top.addView(settingsTool);
        LinearLayout.LayoutParams airGap=new LinearLayout.LayoutParams(dp(78),dp(34));airGap.leftMargin=dp(8);
        top.addView(compactTool("AirPlay",()->airplay()),airGap);
        LinearLayout.LayoutParams customGap=new LinearLayout.LayoutParams(dp(94),dp(34));customGap.leftMargin=dp(8);
        top.addView(compactTool("Customize",()->customization()),customGap);
        root.addView(top);
        TextView title = text("Your devices.",42,white); title.setTypeface(Typeface.create("sans-serif-light",0));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1,dp(65)); titleLp.topMargin=dp(34);
        root.addView(title,titleLp);
        root.addView(text("Pick an input. Enjoy the screen.",17,muted));
        cards = new LinearLayout(this); cards.setClipChildren(false); cards.setClipToPadding(false);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1,dp(182)); rowLp.topMargin=dp(33);
        root.addView(cards,rowLp);
        root.addView(new View(this),new LinearLayout.LayoutParams(-1,0,1));
        TextClock clock = new TextClock(this);clock.setFormat12Hour("h:mm a");clock.setFormat24Hour("HH:mm");
        clock.setTextSize(40);clock.setTextColor(white);clock.setTypeface(Typeface.create("sans-serif-light",0));
        if(!wallpaperKey().isEmpty())clock.setShadowLayer(dp(3),0,dp(1),0xaa000000);
        root.addView(clock,new LinearLayout.LayoutParams(-1,dp(56)));
        status=text("Hold OK on an input to rename it.",12,muted);
        root.addView(status,new LinearLayout.LayoutParams(-1,dp(22)));
        refresh();
    }
    @Override protected void onResume() {
        super.onResume(); inputs.registerCallback(callback,handler); refresh();
    }
    @Override protected void onPause() { inputs.unregisterCallback(callback);handler.removeCallbacks(refreshTask); super.onPause(); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);
        if(customizing && intent.hasCategory(Intent.CATEGORY_HOME))buildHome();
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);wallpaperWorker.shutdownNow();wallpaperCache.evictAll();super.onDestroy();}
    @Override public void onBackPressed() { if(customizing)buildHome(); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_SETTINGS || keyCode==KeyEvent.KEYCODE_NOTIFICATION) { settings(); return true; }
        return super.onKeyDown(keyCode,event);
    }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if(!wallpaperKey().isEmpty())t.setShadowLayer(dp(3),0,dp(1),0xaa000000);
        t.setFontFeatureSettings("kern"); return t;
    }
    private GradientDrawable panel(int color,boolean focus) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(19));
        d.setStroke(dp(focus?2:1),focus?border:isLight()?0x22182235:0x22ffffff); return d;
    }
    private android.graphics.drawable.Drawable surfaceFor(View owner,int color){
        if(glass){
            int[] bg=theme.equals("oled")?new int[]{0xff000000,0xff000000}:
                isLight()?lightGradients[backgroundIndex()]:darkGradients[backgroundIndex()];
            try{owner.setLayerType(View.LAYER_TYPE_HARDWARE,null);return new GlassDrawable(owner,bg,color,isLight(),getResources().getDisplayMetrics().density,photoScene);}
            catch(RuntimeException e){android.util.Log.e("QuietHome","Glass unavailable",e);}
        }
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},panel(color,true));
        states.addState(new int[]{},panel(color,false));return states;
    }
    private void focusSurface(View view,boolean focused){
        if(view.getBackground() instanceof GlassDrawable)((GlassDrawable)view.getBackground()).focus(focused);
        view.animate().cancel();
        view.animate().scaleX(focused?1.035f:1f).scaleY(focused?1.035f:1f).setDuration(100)
            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }
    private TextView compactTool(String label,Runnable action){
        TextView t=tool(label,action);t.setTextSize(12);t.setLayoutParams(new LinearLayout.LayoutParams(dp(86),dp(34)));return t;
    }
    private TextView tool(String label,Runnable action) {
        TextView t=text(label,15,white); t.setGravity(Gravity.CENTER); t.setFocusable(true);
        t.setBackground(surfaceFor(t,surface));
        t.setOnFocusChangeListener((v,f)->focusSurface(v,f));
        t.setOnClickListener(v->action.run());
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(140),dp(44))); return t;
    }
    private void refresh() {
        if(cards==null || customizing)return;
        boolean restoreFocus=getCurrentFocus()==null || getCurrentFocus().getTag()!=null;
        try {
            List<TvInputInfo> all=inputs.getTvInputList();
            StringBuilder signature=new StringBuilder();
            for(TvInputInfo i:all)signature.append(i.getId()).append('|').append(inputs.getInputState(i.getId()))
                .append('|').append(i.loadLabel(this)).append('|').append(i.loadCustomLabel(this)).append(';');
            signature.append(getPreferences(0).getAll().toString());
            if(signature.toString().equals(inputSignature))return;
            inputSignature=signature.toString();cards.removeAllViews();
            List<TvInputInfo> hdmi=new ArrayList<>();
            for(TvInputInfo i:all) if(i.getType()==TvInputInfo.TYPE_HDMI && i.getParentId()==null) hdmi.add(i);
            hdmi.sort(Comparator.comparing(TvInputInfo::getId));
            android.util.Log.i("QuietHome","HDMI inputs: "+hdmi.size());
            View first=null,chosen=null;
            for(int index=0;index<hdmi.size();index++) {
                TvInputInfo base=hdmi.get(index), launch=base;
                for(TvInputInfo child:all) if(base.getId().equals(child.getParentId())) { launch=child; break; }
                final TvInputInfo target=launch;
                String port=portLabel(base,index);
                CharSequence custom=base.loadCustomLabel(this);
                String device=launch!=base?String.valueOf(launch.loadLabel(this)):
                    (custom!=null?custom.toString():port);
                if(device.startsWith("Input Source "))device=port;
                String name=getPreferences(0).getString(base.getId(),device);
                boolean connected=inputs.getInputState(base.getId())==TvInputManager.INPUT_STATE_CONNECTED;
                LinearLayout tile=new LinearLayout(this); tile.setOrientation(1);
                tile.setPadding(dp(20),dp(16),dp(20),dp(14)); tile.setGravity(Gravity.BOTTOM);
                tile.setFocusable(true); tile.setClickable(true); tile.setTag(base.getId());
                int[] colors=isLight()?new int[]{0xffcbdff0,0xffdbd6ee,0xffcce4df,0xffead7cf}:
                    theme.equals("oled")?new int[]{0xff141414,0xff141414,0xff141414,0xff141414}:
                    new int[]{0xff305877,0xff49436b,0xff35595b,0xff66504c}; final int color=colors[index%4];
                tile.setBackground(surfaceFor(tile,connected?color:inactive)); tile.setAlpha(connected?1f:.65f);
                View icon=new ScreenIcon(this); LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(dp(52),dp(43));
                ilp.bottomMargin=dp(12); tile.addView(icon,ilp);
                TextView label=text(name,19,white); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tile.addView(label); TextView sub=text(port+"  ·  "+(connected?"Connected":"No signal"),11,isLight()?muted:0xffced8ea);
                LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.topMargin=dp(7); tile.addView(sub,slp);
                tile.setContentDescription(name+", "+port+", "+(connected?"connected":"no signal"));
                tile.setOnClickListener(v->openInput(target));
                tile.setOnLongClickListener(v->{ rename(base.getId(),name); return true; });
                tile.setOnFocusChangeListener((v,f)->{
                    if(f)focused=base.getId();
                    focusSurface(v,f);
                });
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(160),1); if(index>0)lp.leftMargin=dp(18);
                cards.addView(tile,lp); if(first==null)first=tile; if(base.getId().equals(focused))chosen=tile;
                android.util.Log.i("QuietHome",port+" | "+name+" | connected="+connected+" | "+target.getId());
            }
            if(hdmi.isEmpty()) { TextView empty=text("No HDMI inputs found. Use Settings to check connections.",18,white); cards.addView(empty); }
            if(restoreFocus && first!=null)(chosen!=null?chosen:first).requestFocus();
        } catch(Exception e) { status.setText("Input list unavailable. Settings is still available."); android.util.Log.e("QuietHome","Inputs",e); }
    }
    private String portLabel(TvInputInfo i,int index) {
        // TCL G08 reports physical HDMI 1–4 as HW15–HW18.
        String id=i.getId(); for(int p=1;p<=4;p++)if(id.endsWith("/HW"+(14+p)))return "HDMI "+p;
        return String.valueOf(i.loadLabel(this));
    }
    private void openInput(TvInputInfo info) {
        Intent intent=new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(TvContract.buildChannelUriForPassthroughInput(info.getId()),TvContract.Channels.CONTENT_ITEM_TYPE);
        intent.setComponent(new ComponentName("com.tcl.tv","com.tcl.player.TVActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(intent); } catch(Exception e) { Toast.makeText(this,"Could not open input. Try Settings.",Toast.LENGTH_LONG).show(); android.util.Log.e("QuietHome","Tune",e); }
    }
    private void rename(String id,String current) {
        EditText field=new EditText(this); field.setText(current); field.setSingleLine(true); field.selectAll();
        new AlertDialog.Builder(this).setTitle("Name this input").setView(field)
            .setPositiveButton("Save",(d,w)->{String n=field.getText().toString().trim(); if(!n.isEmpty())getPreferences(0).edit().putString(id,n).apply(); refresh();})
            .setNeutralButton("Automatic",(d,w)->{getPreferences(0).edit().remove(id).apply();refresh();})
            .setNegativeButton("Cancel",null).show();
    }
    private void settings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch(Exception e) { Toast.makeText(this,"Use the TV Settings button.",0).show(); }
    }
    private void customization() {
        customizing=true; cards=null;
        LinearLayout root=page();
        TextView heading=text("Make it yours.",34,white);heading.setTypeface(Typeface.create("sans-serif-light",0));
        root.addView(heading);root.addView(text("Appearance",15,muted));
        LinearLayout themes=new LinearLayout(this);
        String[] names={"Dark","OLED black","Light"}, values={"dark","oled","light"};
        View selected=null;
        for(int n=0;n<3;n++) {
            final String value=values[n];
            TextView button=tool((theme.equals(value)?"✓  ":"")+names[n],()->{
                android.content.SharedPreferences.Editor edit=getPreferences(0).edit().putString("theme",value);if(value.equals("oled"))edit.remove("wallpaper");edit.apply();customization();
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(190),dp(48));lp.rightMargin=dp(15);
            themes.addView(button,lp);if(theme.equals(value))selected=button;
        }
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,dp(55));tlp.topMargin=dp(16);root.addView(themes,tlp);
        TextView backgroundButton=tool("Background · "+backgroundLabel(),()->backgroundPicker());
        LinearLayout.LayoutParams bglp=new LinearLayout.LayoutParams(dp(410),dp(44));bglp.topMargin=dp(12);root.addView(backgroundButton,bglp);
        TextView glassButton=tool("Liquid Glass · "+(glass?"On":"Off"),()->{
            getPreferences(0).edit().putBoolean("liquid_glass",!glass).apply();customization();
            getWindow().getDecorView().findViewWithTag("glass-toggle").requestFocus();
        });
        glassButton.setTag("glass-toggle");glassButton.setEnabled(Build.VERSION.SDK_INT>=33);
        LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(dp(410),dp(44));glp.topMargin=dp(10);root.addView(glassButton,glp);
        if(!wallpaperKey().isEmpty()){
            int shade=getPreferences(0).getInt("wallpaper_shade",30);
            TextView shadeLabel=text("Wallpaper darkening · "+shade+"%",16,white);
            LinearLayout.LayoutParams shadeLp=new LinearLayout.LayoutParams(-1,-2);shadeLp.topMargin=dp(15);root.addView(shadeLabel,shadeLp);
            SeekBar shadeSlider=new SeekBar(this);shadeSlider.setMax(70);shadeSlider.setProgress(shade);shadeSlider.setKeyProgressIncrement(5);
            shadeSlider.setTag("wallpaper-shade");shadeSlider.setContentDescription("Wallpaper darkening");
            root.addView(shadeSlider,new LinearLayout.LayoutParams(dp(650),dp(42)));
            shadeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                private final Runnable apply=()->{customization();View v=getWindow().getDecorView().findViewWithTag("wallpaper-shade");if(v!=null)v.requestFocus();};
                public void onStartTrackingTouch(SeekBar s){}
                public void onStopTrackingTouch(SeekBar s){}
                public void onProgressChanged(SeekBar s,int value,boolean user){if(!user)return;
                    shadeLabel.setText("Wallpaper darkening · "+value+"%");getPreferences(0).edit().putInt("wallpaper_shade",value).apply();
                    handler.removeCallbacks(apply);handler.postDelayed(apply,350);
                }
            });
        }
        TextView title=text("TV brightness",22,white);
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,-2);blp.topMargin=dp(26);root.addView(title,blp);
        TextView readout=text("Reading panel backlight…",14,muted);root.addView(readout);
        SeekBar slider=new SeekBar(this);slider.setMin(1);slider.setMax(100);slider.setKeyProgressIncrement(5);
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(isLight()?0xff305c91:0xffb9d6ff));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(white));
        slider.setContentDescription("TV brightness. Use left and right to adjust.");
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(dp(650),dp(48));slp.topMargin=dp(8);root.addView(slider,slp);
        try {
            int current=readBacklight();slider.setProgress(current);
            readout.setText(current+"% · Use left / right to adjust");
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onStartTrackingTouch(SeekBar s){}
                public void onStopTrackingTouch(SeekBar s){}
                public void onProgressChanged(SeekBar s,int value,boolean user){
                    if(!user)return;
                    try { writeBacklight(value);int actual=readBacklight();readout.setText(actual+"% · Current picture mode"); }
                    catch(Exception e){readout.setText("TV blocked this adjustment. Use TV Settings.");android.util.Log.e("QuietHome","Backlight write",e);}
                }
            });
        } catch(Exception e){slider.setEnabled(false);readout.setText("Direct control unavailable. Open TV Settings to adjust brightness.");android.util.Log.e("QuietHome","Backlight read",e);}
        root.addView(text("Changes the backlight for the current picture mode. HDR and other inputs may use their own settings.",12,muted));
        LinearLayout actions=new LinearLayout(this);
        actions.addView(tool("TV Settings",()->settings()));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(dp(140),dp(44));gap.leftMargin=dp(14);
        actions.addView(tool("Done",()->buildHome()),gap);
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,dp(44));alp.topMargin=dp(24);root.addView(actions,alp);
        if(selected!=null)selected.requestFocus();
    }
    private void backgroundPicker(){
        Context dialogContext=new ContextThemeWrapper(this,isLight()?
            android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert);
        String[] options=new String[backgroundNames.length+wallpaperNames.length];
        for(int i=0;i<backgroundNames.length;i++)options[i]=backgroundNames[i]+" · Gradient";
        int selected=theme.equals("oled")?-1:backgroundIndex();
        for(int i=0;i<wallpaperNames.length;i++){
            options[backgroundNames.length+i]=wallpaperNames[i]+" · Landscape";
            if(wallpaperKey().equals(wallpaperFiles[i]))selected=backgroundNames.length+i;
        }
        new AlertDialog.Builder(dialogContext).setTitle("Background")
            .setSingleChoiceItems(options,selected,(dialog,which)->{
                android.content.SharedPreferences.Editor edit=getPreferences(0).edit();
                if(which<backgroundNames.length)edit.putInt("background",which).remove("wallpaper");
                else edit.putString("wallpaper",wallpaperFiles[which-backgroundNames.length]);
                if(theme.equals("oled"))edit.putString("theme","dark");
                edit.apply();dialog.dismiss();customization();
            }).setNegativeButton("Cancel",null).show();
    }
    private void initBacklight() throws Exception {
        if(backlight!=null)return;
        backlightClass=Class.forName("com.tcl.tv.pq.TvBacklightManager");
        applyClass=Class.forName("com.tcl.tv.pq.EnApplyType");actClass=Class.forName("com.tcl.tv.pq.EnTclActType");
        backlight=backlightClass.getMethod("getInstance").invoke(null);
    }
    private int readBacklight() throws Exception {
        initBacklight();int value=(Integer)backlightClass.getMethod("getBacklight").invoke(backlight);
        if(value<0 || value>100)throw new IllegalStateException("Invalid backlight: "+value);
        android.util.Log.i("QuietHome","Panel backlight="+value);return value;
    }
    private void writeBacklight(int value) throws Exception {
        initBacklight();
        Object apply=applyClass.getField("EN_APPLY_CURRENT").get(null), act=actClass.getField("EN_TCL_ACT_EXEC_SAVE").get(null);
        Object result=backlightClass.getMethod("setBacklight",applyClass,int.class,actClass).invoke(backlight,apply,value,act);
        if(Boolean.FALSE.equals(result))throw new IllegalStateException("Backlight rejected");
        android.util.Log.i("QuietHome","Set panel backlight="+value);
    }
    private void airplay() {
        for(TvInputInfo i:inputs.getTvInputList())if(i.getId().contains("AirPlayTvInputService")){openInput(i);return;}
        Intent i=getPackageManager().getLeanbackLaunchIntentForPackage("com.tcl.airplay2");
        if(i!=null)startActivity(i);else settings();
    }
    private class ScreenIcon extends View {
        Paint paint=new Paint(3); ScreenIcon(Context c){super(c);}
        @Override protected void onDraw(Canvas c) {
            paint.setColor(white);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));
            float w=getWidth(),h=getHeight(); c.drawRoundRect(dp(2),dp(2),w-dp(2),h-dp(10),dp(5),dp(5),paint);
            c.drawLine(w/2,h-dp(10),w/2,h-dp(3),paint);c.drawLine(w*.3f,h-dp(2),w*.7f,h-dp(2),paint);
        }
    }
}
