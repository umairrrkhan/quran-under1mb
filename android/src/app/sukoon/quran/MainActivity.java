package app.sukoon.quran;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
  SharedPreferences prefs;
  JSONObject[] index=new JSONObject[114];
  int[] read=new int[114];
  Set<String> bookmarks=new HashSet<>();
  ExecutorService worker=Executors.newSingleThreadExecutor();
  LinkedHashMap<Integer,JSONArray> cache=new LinkedHashMap<Integer,JSONArray>(4,.75f,true){
    protected boolean removeEldestEntry(Map.Entry<Integer,JSONArray> e){return size()>3;}
  };
  JSONArray ayahs;
  LinearLayout root, content, header;
  ListView list;
  TextView positionLabel;
  BitSet[] viewed=new BitSet[114];
  boolean progressSaving=false;
  TextView markButton;
  ProgressBar chapterProgress;
  BaseAdapter adapter;
  ArrayList<Integer> rows=new ArrayList<>();
  ArrayList<String[]> savedRows=new ArrayList<>();
  int chapter=-1, tab=0, language=0, font=30, theme=0, request=0;
  int bg,ink,secondary,line,shade; boolean dark,destroyed,loading=false,ready=false;
  String query="";
  int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  String name(int c){return index[c].optString("name");}
  int count(int c){return index[c].optInt("count");}
  String key(int c,int v){return c+":"+v;}
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    prefs=getSharedPreferences("reader2",0);
    language=Math.max(0,Math.min(4,prefs.getInt("language",0)));
    font=Math.max(24,Math.min(42,prefs.getInt("font",30)));
    theme=prefs.getInt("theme",0);bookmarks=new HashSet<>(prefs.getStringSet("bookmarks",new HashSet<>()));
    try {
      JSONArray all=new JSONArray(asset("index.json"));
      for(int i=0;i<114;i++){index[i]=all.getJSONObject(i);read[i]=Math.max(0,Math.min(count(i),prefs.getInt("read"+i,0)));
        String stored=prefs.getString("viewed"+i,null);viewed[i]=new BitSet(count(i));
        if(stored==null)viewed[i].set(0,read[i]);else for(String item:stored.split(",")){try{int n=Integer.parseInt(item);if(n>=0&&n<count(i))viewed[i].set(n);}catch(NumberFormatException ignored){}}
        read[i]=viewed[i].cardinality();}
    }catch(Exception e){new AlertDialog.Builder(this).setTitle("Unable to open Quran").setMessage("Please reinstall the APK.").setPositiveButton("Close",(d,w)->finish()).show();return;}
    if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,()->back());
    int restore=state==null?-1:state.getInt("chapter",-1);
    tab=state==null?0:state.getInt("tab",0);ready=true;
    if(restore>=0)open(restore,state.getInt("verse",0),state.getInt("offset",0));else home(tab);
  }
  String asset(String path)throws Exception{
    try(InputStream in=getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");
    }
  }
  int packedGroup=-1;
  String[] packedChapters;
  JSONArray loadChapter(int c)throws Exception{
    JSONArray a=cache.get(c);if(a==null){int group=c/40;
      if(packedGroup!=group){
        String[] decoded=new String[Math.min(40,114-group*40)];
        try(java.io.DataInputStream input=new java.io.DataInputStream(new org.tukaani.xz.XZInputStream(getAssets().open("group"+group+".xz")))){
          for(int i=0;i<decoded.length;i++){int length=input.readInt();byte[] bytes=new byte[length];input.readFully(bytes);decoded[i]=new String(bytes,"UTF-8");}
        }
        packedChapters=decoded;packedGroup=group;
      }
      a=new JSONArray(packedChapters[c%40]);cache.put(c,a);}return a;
  }
  void colors(){
    dark=theme==2||(theme==0&&(getResources().getConfiguration().uiMode&48)==32);
    setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);
    bg=Color.parseColor(dark?"#151515":"#FAFAF8");
    ink=Color.parseColor(dark?"#EEEEEA":"#20201E");
    secondary=Color.parseColor(dark?"#A2A29D":"#74746F");
    line=Color.parseColor(dark?"#343431":"#E3E3DD");
    shade=Color.parseColor(dark?"#242422":"#F0F0EB");
  }
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  TextView txt(String s,int size,int color){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(true);return t;
  }
  void medium(TextView t){t.setTypeface(Typeface.create("sans-serif-medium",0));}
  Drawable ripple(){return new RippleDrawable(ColorStateList.valueOf(dark?0x22FFFFFF:0x15000000),null,new ColorDrawable(Color.WHITE));}
  TextView action(String s,View.OnClickListener fn){
    TextView t=txt(s,14,ink);medium(t);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(48));t.setPadding(dp(12),0,dp(12),0);t.setBackground(ripple());t.setOnClickListener(fn);t.setFocusable(true);return t;
  }
  View rule(){View v=new View(this);v.setBackgroundColor(line);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}
  TextView caption(String s){TextView t=txt(s,11,secondary);t.setLetterSpacing(.12f);return t;}
  class Icon extends View {
    final String type;final Paint p=new Paint(3);
    Icon(String kind,String desc,Runnable run){super(MainActivity.this);type=kind;setContentDescription(desc);setBackground(ripple());setFocusable(true);setOnClickListener(v->run.run());}
    @Override protected void onDraw(Canvas canvas){
      super.onDraw(canvas);canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);canvas.scale(dp(1),dp(1));
      p.setColor(ink);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
      if(type.equals("back")){canvas.drawLine(7,0,-7,0,p);canvas.drawLine(-7,0,-1,-6,p);canvas.drawLine(-7,0,-1,6,p);}
      else if(type.equals("more")){p.setStyle(Paint.Style.FILL);for(int i=-1;i<=1;i++)canvas.drawCircle(0,i*6,1.5f,p);}
      else if(type.equals("next")){canvas.drawLine(-7,0,7,0,p);canvas.drawLine(7,0,1,-6,p);canvas.drawLine(7,0,1,6,p);}
      else {Path path=new Path();path.moveTo(-6,-9);path.lineTo(6,-9);path.lineTo(6,9);path.lineTo(0,5);path.lineTo(-6,9);path.close();canvas.drawPath(path,p);}
      canvas.restore();
    }
  }
  void shell(){
    colors();root=col();root.setBackgroundColor(bg);
    setContentView(root);configureSystemBars();root.requestApplyInsets();
    header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(12),dp(10),dp(12),dp(8));
    if(chapter>=0)header.addView(new Icon("back","Back to surahs",()->home(0)),new LinearLayout.LayoutParams(dp(48),dp(48)));
    LinearLayout titles=col();titles.setPadding(dp(chapter>=0?0:12),0,dp(4),0);
    TextView title=txt(chapter>=0?name(chapter):"Quran",chapter>=0?20:32,ink);medium(title);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);
    titles.addView(title);
    TextView sub=caption(chapter>=0?(chapter+1)+"  /  "+index[chapter].optString("meaning").toUpperCase(Locale.ROOT):"114 SURAHS  ·  OFFLINE");
    sub.setSingleLine(true);sub.setEllipsize(TextUtils.TruncateAt.END);titles.addView(sub);
    header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
    TextView settings=action("Aa",v->settings());settings.setTextSize(18);settings.setContentDescription("Reading settings");header.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));
    header.addView(new Icon("more","More options",()->menu()),new LinearLayout.LayoutParams(dp(48),dp(48)));
    root.addView(header);
  }
  void configureSystemBars(){
    Window window=getWindow();
    if(Build.VERSION.SDK_INT>=30){
      window.setDecorFitsSystemWindows(false);
      root.setOnApplyWindowInsetsListener((v,i)->{Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(x.left,x.top,x.right,x.bottom);return i;});
      if(window.getInsetsController()!=null)window.getInsetsController().setSystemBarsAppearance(dark?0:24,24);
    }else{
      int flags=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
      if(!dark)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      if(!dark&&Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      window.getDecorView().setSystemUiVisibility(flags);
      root.setOnApplyWindowInsetsListener((v,i)->{root.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
    }
    window.setStatusBarColor(Color.TRANSPARENT);
    window.setNavigationBarColor(Build.VERSION.SDK_INT<26?Color.BLACK:Color.TRANSPARENT);
  }
  int totalRead(){int n=0;for(int x:read)n+=x;return n;}
  int completed(){int n=0;for(int i=0;i<114;i++)if(read[i]>=count(i))n++;return n;}
  String percent(int n,int total){return String.format(Locale.US,"%.1f%%",100.0*n/total);}
  void home(int newTab){
    savePosition();request++;chapter=-1;ayahs=null;loading=false;tab=newTab;query="";shell();
    LinearLayout tabs=new LinearLayout(this);tabs.setPadding(dp(24),dp(10),dp(24),0);
    String[] labels={"Surahs","Saved","Progress"};
    for(int i=0;i<3;i++){final int t=i;LinearLayout wrap=col();TextView b=action(labels[i],v->home(t));b.setTextColor(i==tab?ink:secondary);wrap.addView(b);View underline=new View(this);underline.setBackgroundColor(i==tab?ink:Color.TRANSPARENT);wrap.addView(underline,new LinearLayout.LayoutParams(-1,dp(2)));tabs.addView(wrap,new LinearLayout.LayoutParams(0,-2,1));}
    root.addView(tabs);root.addView(rule());
    if(tab==0){
      int c=prefs.getInt("lastChapter",-1);
      if(c>=0&&c<114){
        LinearLayout resume=new LinearLayout(this);resume.setGravity(Gravity.CENTER_VERTICAL);resume.setPadding(dp(24),dp(18),dp(14),dp(18));
        LinearLayout detail=col();detail.addView(caption("CONTINUE READING"));
        TextView title=txt(name(c)+"  ·  "+(prefs.getInt("lastVerse",0)+1),18,ink);title.setPadding(0,dp(6),0,0);detail.addView(title);
        resume.addView(detail,new LinearLayout.LayoutParams(0,-2,1));resume.addView(new Icon("next","Continue reading",()->resume()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        resume.setBackground(ripple());resume.setOnClickListener(v->resume());root.addView(resume);root.addView(rule());
      }
      EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(16);search.setTextColor(ink);search.setHintTextColor(secondary);search.setHint("Search surahs");search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(dp(24),dp(4),dp(24),0);
      root.addView(search,new LinearLayout.LayoutParams(-1,dp(58)));
      search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int st,int b,int c){query=s.toString().trim().toLowerCase(Locale.ROOT);filter();}public void afterTextChanged(Editable e){}});
    }
    if(tab==2){
      LinearLayout summary=col();summary.setPadding(dp(24),dp(26),dp(24),dp(20));summary.addView(caption("YOUR READING"));
      LinearLayout numbers=new LinearLayout(this);numbers.setGravity(Gravity.BOTTOM);
      TextView pct=txt(percent(totalRead(),6236),40,ink);medium(pct);numbers.addView(pct,new LinearLayout.LayoutParams(0,-2,1));
      TextView done=txt(completed()+" / 114 surahs",13,secondary);done.setPadding(0,0,0,dp(8));numbers.addView(done);summary.addView(numbers);
      ProgressBar progress=progress(totalRead(),6236);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(3));pp.setMargins(0,dp(12),0,dp(14));summary.addView(progress,pp);
      summary.addView(txt(totalRead()+" of 6,236 verses read",14,secondary));TextView note=txt("Use Mark read to confirm the verses you have finished.",12,secondary);note.setPadding(0,dp(5),0,0);summary.addView(note);root.addView(summary);root.addView(rule());
    }
    createList();
    if(tab==1)loadSaved();else filter();
    list.setOnItemClickListener((a,v,p,id)->{if(tab==1){String[] s=savedRows.get(p);open(Integer.parseInt(s[0]),Integer.parseInt(s[1]),0);}else{int c=rows.get(p);open(c,tab==2?Math.min(prefs.getInt("pos"+c,0),count(c)-1):0,0);}});
    list.setOnItemLongClickListener((a,v,p,id)->{if(tab==1){String[] s=savedRows.get(p);int c=Integer.parseInt(s[0]),n=Integer.parseInt(s[1]);new AlertDialog.Builder(this).setTitle(name(c)+" "+(c+1)+":"+(n+1)).setItems(new String[]{"Remove bookmark"},(d,w)->{bookmarks.remove(key(c,n));persistBookmarks();loadSaved();}).show();return true;}return false;});
  }
  void resume(){int c=prefs.getInt("lastChapter",0);open(c,prefs.getInt("lastVerse",0),prefs.getInt("lastOffset",0));}
  ProgressBar progress(int value,int max){
    ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(max);p.setProgress(value);p.setProgressTintList(ColorStateList.valueOf(ink));p.setProgressBackgroundTintList(ColorStateList.valueOf(line));return p;
  }
  void createList(){
    content=col();root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
    list=new ListView(this);list.setDivider(null);list.setCacheColorHint(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(false);list.setClipToPadding(false);list.setPadding(dp(24),0,dp(24),dp(20));list.setSelector(new ColorDrawable(Color.TRANSPARENT));
    content.addView(list,new LinearLayout.LayoutParams(-1,0,1));
    TextView empty=txt(tab==1?"No saved verses yet.\nHold a verse while reading to save it.":tab==2?"Your reading progress will appear here.":"No surahs found.",15,secondary);
    empty.setGravity(Gravity.CENTER);empty.setPadding(dp(24),dp(36),dp(24),dp(36));empty.setLineSpacing(dp(8),1);content.addView(empty,new LinearLayout.LayoutParams(-1,0,1));list.setEmptyView(empty);
    adapter=new BaseAdapter(){
      public int getCount(){return chapter>=0?(ayahs==null?0:ayahs.length()):tab==1?savedRows.size():rows.size();}
      public Object getItem(int p){return p;}public long getItemId(int p){return p;}
      public View getView(int p,View old,ViewGroup parent){return chapter>=0?verseRow(p,old):tab==1?savedRow(p,old):surahRow(rows.get(p),old);}
    };list.setAdapter(adapter);
  }
  void filter(){
    if(chapter>=0||tab==1||adapter==null)return;rows.clear();
    for(int i=0;i<114;i++){
      if(tab==2&&read[i]==0)continue;
      String q=(name(i)+" "+index[i].optString("hi")+" "+index[i].optString("ar")+" "+(i+1)).toLowerCase(Locale.ROOT);
      if(q.contains(query))rows.add(i);
    }adapter.notifyDataSetChanged();
  }
  View surahRow(int c,View old){
    LinearLayout row;
    if(old instanceof LinearLayout)row=(LinearLayout)old;else{
      row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(19),0,dp(19));row.setBackground(ripple());
      TextView num=txt("",12,secondary);row.addView(num,new LinearLayout.LayoutParams(dp(34),-2));
      LinearLayout text=col();text.addView(txt("",17,ink));TextView small=txt("",12,secondary);small.setPadding(0,dp(4),0,0);text.addView(small);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
      TextView arabic=txt("",23,ink);arabic.setGravity(Gravity.RIGHT);arabic.setTextDirection(View.TEXT_DIRECTION_RTL);arabic.setMaxWidth(dp(150));arabic.setPadding(dp(8),0,0,0);row.addView(arabic);
    }
    ((TextView)row.getChildAt(0)).setText(String.format(Locale.ROOT,"%02d",c+1));
    LinearLayout text=(LinearLayout)row.getChildAt(1);((TextView)text.getChildAt(0)).setText(name(c));
    ((TextView)text.getChildAt(1)).setText(tab==2?read[c]+" / "+count(c)+" read"+(read[c]==count(c)?" · Complete":""):count(c)+" verses"+(read[c]>0?" · "+percent(read[c],count(c)):""));
    ((TextView)row.getChildAt(2)).setText(index[c].optString("ar"));return row;
  }
  View savedRow(int n,View old){
    LinearLayout row;if(old instanceof LinearLayout)row=(LinearLayout)old;else{
      row=col();row.setPadding(0,dp(20),0,dp(20));row.setBackground(ripple());row.addView(txt("",17,ink));
      TextView preview=txt("",14,secondary);preview.setMaxLines(2);preview.setEllipsize(TextUtils.TruncateAt.END);preview.setPadding(0,dp(8),0,0);preview.setLineSpacing(dp(4),1);row.addView(preview);
    }
    String[] s=savedRows.get(n);((TextView)row.getChildAt(0)).setText(name(Integer.parseInt(s[0]))+"  "+(Integer.parseInt(s[1])+1));
    ((TextView)row.getChildAt(1)).setText(s[2]);return row;
  }
  void loadSaved(){
    int token=++request;savedRows.clear();adapter.notifyDataSetChanged();Set<String> copy=new HashSet<>(bookmarks);final int lang=language;
    worker.execute(()->{
      ArrayList<String[]> result=new ArrayList<>();
      try{
        ArrayList<Integer> refs=new ArrayList<>();for(String k:copy){String[] bits=k.split(":");refs.add(Integer.parseInt(bits[0])*1000+Integer.parseInt(bits[1]));}Collections.sort(refs);
        for(int ref:refs){int c=ref/1000,n=ref%1000;JSONObject verse=loadChapter(c).getJSONObject(n);result.add(new String[]{""+c,""+n,verse.optString(lang==1||lang==4?"h":"e")});}
        runOnUiThread(()->{if(!destroyed&&request==token&&chapter<0&&tab==1){savedRows=result;adapter.notifyDataSetChanged();}});
      }catch(Exception e){runOnUiThread(()->{if(!destroyed&&request==token)Toast.makeText(this,"Could not load saved verses.",Toast.LENGTH_SHORT).show();});}
    });
  }
  void open(int c,int verse,int offset){
    if(c<0||c>113)return;savePosition();
    if(root!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);
    chapter=c;ayahs=null;loading=true;final int token=++request;shell();
    TextView wait=txt("Opening surah…",14,secondary);wait.setGravity(Gravity.CENTER);root.addView(wait,new LinearLayout.LayoutParams(-1,0,1));
    worker.execute(()->{
      try{JSONArray data=loadChapter(c);runOnUiThread(()->{
        if(destroyed||request!=token)return;ayahs=data;loading=false;renderReader(Math.max(0,Math.min(count(c)-1,verse)),offset);
      });}catch(Exception e){runOnUiThread(()->{if(!destroyed&&request==token){home(0);Toast.makeText(this,"Could not open surah. Please try again.",Toast.LENGTH_LONG).show();}});}
    });
  }
  String modeLabel(){return new String[]{"Arabic + English","Arabic + Hindi","Arabic only","English only","Hindi only"}[language];}
  void renderReader(int verse,int offset){
    positionLabel=null;shell();
    LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);controls.setPadding(dp(12),0,dp(12),0);
    TextView translation=action(modeLabel()+"  ⌄",v->chooseLanguage());translation.setTextSize(13);translation.setTextColor(secondary);translation.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
    controls.addView(translation,new LinearLayout.LayoutParams(0,dp(48),1));
    TextView jump=action("Go to verse",v->jump());jump.setTextSize(13);jump.setTextColor(secondary);controls.addView(jump);root.addView(controls);root.addView(rule());
    createList();
    list.setOnItemLongClickListener((a,v,p,id)->{verseMenu(p);return true;});
    list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updateFooter());
    list.setOnScrollListener(new AbsListView.OnScrollListener(){
      public void onScrollStateChanged(AbsListView v,int state){if(state==0)savePosition();}
      public void onScroll(AbsListView v,int first,int visible,int total){if(positionLabel!=null&&chapter>=0&&!loading)updateFooter();}
    });
    chapterProgress=progress(read[chapter],count(chapter));root.addView(chapterProgress,new LinearLayout.LayoutParams(-1,dp(2)));
    LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);footer.setPadding(dp(24),0,dp(12),0);
    positionLabel=txt("",12,secondary);footer.addView(positionLabel,new LinearLayout.LayoutParams(0,dp(52),1));positionLabel.setGravity(Gravity.CENTER_VERTICAL);
    markButton=action("Mark read",v->chooseReadVerses());footer.addView(markButton);root.addView(footer);
    list.getViewTreeObserver().addOnPreDrawListener(()->{updateFooter();return true;});
    list.setSelectionFromTop(verse,offset);prefs.edit().putInt("lastChapter",chapter).putInt("lastVerse",verse).putInt("lastOffset",offset).apply();
    updateFooter();
  }
  int anchorVerse(){
    if(list!=null&&list.getChildCount()>0&&list.getChildAt(0).getTag() instanceof Integer)return (Integer)list.getChildAt(0).getTag();
    return Math.max(0,Math.min(count(chapter)-1,prefs.getInt("lastVerse",0)));
  }
  int[] visibleVerses(){
    int first=-1,last=-1;Rect clip=new Rect();
    if(list==null||!list.getLocalVisibleRect(clip))return new int[]{-1,-1};
    for(int i=0;i<list.getChildCount();i++){
      View item=list.getChildAt(i);if(!(item instanceof LinearLayout)||!(item.getTag() instanceof Integer))continue;
      LinearLayout row=(LinearLayout)item;boolean visible=false;
      for(int t=1;t<=2;t++){
        TextView text=(TextView)row.getChildAt(t);android.text.Layout layout=text.getLayout();
        if(text.getVisibility()!=View.VISIBLE||layout==null)continue;
        int origin=row.getTop()+text.getTop()+text.getTotalPaddingTop();
        for(int n=0;n<layout.getLineCount();n++)if(origin+layout.getLineBottom(n)>clip.top&&origin+layout.getLineTop(n)<clip.bottom){visible=true;break;}
      }
      if(visible){int v=(Integer)row.getTag();if(first<0)first=v;last=v;}
    }return new int[]{first,last};
  }
  int currentVerse(){int[] range=visibleVerses();return range[0]>=0?range[0]:anchorVerse();}
  void updateFooter(){
    if(positionLabel==null||chapter<0||loading)return;
    int[] range=visibleVerses();
    String visible=range[0]<0?"Reading position saved":range[0]==range[1]?"Verse "+(range[0]+1)+" of "+count(chapter):"Verses "+(range[0]+1)+"–"+(range[1]+1)+" of "+count(chapter);
    positionLabel.setText(visible+"\nRead: "+read[chapter]+" / "+count(chapter));
    if(markButton!=null)markButton.setText(read[chapter]==count(chapter)?"Edit progress":"Mark read");
  }
  void chooseReadVerses(){
    if(chapter<0||loading||progressSaving)return;
    final int target=chapter;int[] range=visibleVerses();
    if(range[0]<0){chooseReadNumber();return;}
    final int first=range[0];final boolean[] checked=new boolean[range[1]-first+1];
    String[] labels=new String[checked.length];
    for(int i=0;i<labels.length;i++){int v=first+i;labels[i]="Verse "+(v+1)+(viewed[target].get(v)?" · Read":"");checked[i]=true;}
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Mark read · "+name(target))
      .setMultiChoiceItems(labels,checked,(d,n,value)->checked[n]=value)
      .setNegativeButton("Cancel",null).setNeutralButton("Finish surah",(d,w)->confirmFinishSurah())
      .setPositiveButton("Save",null).create();
    dialog.show();dialog.getButton(-1).setOnClickListener(v->{
      BitSet selection=(BitSet)viewed[target].clone();
      for(int i=0;i<checked.length;i++)selection.set(first+i,checked[i]);
      saveReadProgress(target,selection,dialog);
    });
  }
  void chooseReadNumber(){
    if(chapter<0||progressSaving)return;final int target=chapter;
    EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(2);input.setHint("1–"+count(target));input.setPadding(dp(24),dp(16),dp(24),dp(16));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Mark one verse read").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();dialog.show();
    dialog.getButton(-1).setOnClickListener(v->{try{int number=Integer.parseInt(input.getText().toString());if(number<1||number>count(target))throw new Exception();BitSet selection=(BitSet)viewed[target].clone();selection.set(number-1);saveReadProgress(target,selection,dialog);}catch(Exception e){input.setError("Enter a verse from 1 to "+count(target));}});
  }
  void confirmFinishSurah(){
    if(chapter<0||loading||progressSaving)return;final int target=chapter;
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Finish "+name(target)+"?").setMessage("Mark all "+count(target)+" verses in this surah as read.")
      .setNegativeButton("Cancel",null).setPositiveButton("Finish",null).create();dialog.show();
    dialog.getButton(-1).setOnClickListener(v->{BitSet selection=new BitSet(count(target));selection.set(0,count(target));saveReadProgress(target,selection,dialog);});
  }
  void saveReadProgress(int target,BitSet selection,AlertDialog dialog){
    if(progressSaving)return;progressSaving=true;for(int button:new int[]{-1,-2,-3})if(dialog.getButton(button)!=null)dialog.getButton(button).setEnabled(false);dialog.setCancelable(false);
    final BitSet saved=(BitSet)selection.clone();
    StringBuilder value=new StringBuilder();for(int n=saved.nextSetBit(0);n>=0;n=saved.nextSetBit(n+1)){if(value.length()>0)value.append(',');value.append(n);}
    final String serialized=value.toString();
    worker.execute(()->{
      boolean stored=prefs.edit().putString("viewed"+target,serialized).putInt("read"+target,saved.cardinality()).commit();
      runOnUiThread(()->{
        progressSaving=false;if(destroyed)return;
        if(stored){
          viewed[target]=saved;read[target]=saved.cardinality();dialog.dismiss();
          if(chapter==target&&chapterProgress!=null){chapterProgress.setProgress(read[target]);updateFooter();}
          else if(chapter<0&&tab==2)home(2);
          Toast.makeText(this,"Saved · "+read[target]+" of "+count(target)+" verses read",Toast.LENGTH_SHORT).show();
        }else{dialog.setCancelable(true);for(int button:new int[]{-1,-2,-3})if(dialog.getButton(button)!=null)dialog.getButton(button).setEnabled(true);Toast.makeText(this,"Could not save progress. Please try again.",Toast.LENGTH_LONG).show();}
      });
    });
  }
  View verseRow(int v,View old){
    LinearLayout row;
    if(old instanceof LinearLayout)row=(LinearLayout)old;else{
      row=col();row.setPadding(0,dp(22),0,0);
      TextView number=txt("",12,secondary);number.setMinHeight(dp(26));row.addView(number,new LinearLayout.LayoutParams(-1,-2));
      TextView ar=txt("",font,ink);ar.setGravity(Gravity.RIGHT);ar.setTextDirection(View.TEXT_DIRECTION_RTL);ar.setLineSpacing(dp(10),1.15f);ar.setPadding(0,dp(13),0,dp(18));row.addView(ar,new LinearLayout.LayoutParams(-1,-2));
      TextView tr=txt("",17,ink);tr.setLineSpacing(dp(6),1.1f);tr.setPadding(0,0,0,dp(26));row.addView(tr,new LinearLayout.LayoutParams(-1,-2));
      row.addView(rule());row.setLongClickable(true);
    }
    JSONObject a=ayahs.optJSONObject(v);boolean saved=bookmarks.contains(key(chapter,v));
    row.setTag(v);
    TextView number=(TextView)row.getChildAt(0);number.setText((chapter+1)+":"+(v+1)+(saved?"   ·   Saved":""));
    TextView ar=(TextView)row.getChildAt(1);ar.setText(a.optString("a"));ar.setTextSize(font);ar.setVisibility(language<=2?View.VISIBLE:View.GONE);
    TextView tr=(TextView)row.getChildAt(2);tr.setText(a.optString(language==1||language==4?"h":"e"));tr.setTextSize(Math.max(16,font-13));tr.setVisibility(language==2?View.GONE:View.VISIBLE);
    row.setOnLongClickListener(x->{verseMenu(v);return true;});return row;
  }
  void persistBookmarks(){prefs.edit().putStringSet("bookmarks",new HashSet<>(bookmarks)).apply();}
  void toggleBookmark(int v){
    String k=key(chapter,v);boolean add=!bookmarks.remove(k);if(add)bookmarks.add(k);persistBookmarks();adapter.notifyDataSetChanged();
    Toast.makeText(this,add?"Verse saved":"Bookmark removed",Toast.LENGTH_SHORT).show();
  }
  void verseMenu(int v){
    if(loading||ayahs==null)return;
    new AlertDialog.Builder(this).setTitle(name(chapter)+" "+(chapter+1)+":"+(v+1)).setItems(new String[]{bookmarks.contains(key(chapter,v))?"Remove bookmark":"Save verse","Copy verse"},(d,w)->{
      if(w==0)toggleBookmark(v);else{
        JSONObject a=ayahs.optJSONObject(v);String copy=(language<=2?a.optString("a")+"\n\n":"")+(language!=2?a.optString(language==1||language==4?"h":"e")+"\n\n":"")+"Quran "+(chapter+1)+":"+(v+1);
        ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Quran verse",copy));Toast.makeText(this,"Verse copied",Toast.LENGTH_SHORT).show();
      }
    }).show();
  }
  void savePosition(){
    if(!ready||chapter<0||loading||ayahs==null||list==null)return;
    prefs.edit().putInt("lastChapter",chapter).putInt("pos"+chapter,anchorVerse()).putInt("lastVerse",anchorVerse()).putInt("lastOffset",list.getChildCount()>0?list.getChildAt(0).getTop()-list.getPaddingTop():0).apply();
  }
  void chooseLanguage(){
    new AlertDialog.Builder(this).setTitle("Reading language").setSingleChoiceItems(new String[]{"Arabic + English","Arabic + Hindi","Arabic only","English only","Hindi only"},language,(d,w)->{
      savePosition();language=w;prefs.edit().putInt("language",w).apply();d.dismiss();renderReader(prefs.getInt("lastVerse",0),prefs.getInt("lastOffset",0));
    }).setNegativeButton("Cancel",null).show();
  }
  void jump(){
    if(loading)return;EditText input=new EditText(this);input.setInputType(2);input.setSingleLine(true);input.setHint("1–"+count(chapter));input.setPadding(dp(24),dp(16),dp(24),dp(16));
    AlertDialog d=new AlertDialog.Builder(this).setTitle("Go to verse").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Go",null).create();d.show();
    d.getButton(-1).setOnClickListener(v->{try{int n=Integer.parseInt(input.getText().toString());if(n<1||n>count(chapter))throw new Exception();list.setSelectionFromTop(n-1,0);prefs.edit().putInt("lastChapter",chapter).putInt("lastVerse",n-1).putInt("lastOffset",0).apply();d.dismiss();}catch(Exception e){input.setError("Enter a verse from 1 to "+count(chapter));}});
  }
  void settings(){
    if(loading)return;LinearLayout box=col();box.setPadding(dp(24),dp(12),dp(24),dp(12));
    box.addView(txt("Appearance",14,secondary));RadioGroup options=new RadioGroup(this);
    String[] labels={"System","Light","Dark"};for(int i=0;i<3;i++){RadioButton b=new RadioButton(this);b.setId(100+i);b.setText(labels[i]);b.setTextColor(ink);options.addView(b);}options.check(100+theme);box.addView(options);
    TextView label=txt("Text size",14,secondary);label.setPadding(0,dp(18),0,dp(8));box.addView(label);
    SeekBar size=new SeekBar(this);size.setMax(18);size.setProgress(font-24);size.setProgressTintList(ColorStateList.valueOf(ink));size.setThumbTintList(ColorStateList.valueOf(ink));box.addView(size);
    TextView sample=txt("بِسْمِ اللَّهِ\nIn the name of Allah\nअल्लाह के नाम से",font-10,ink);sample.setGravity(Gravity.CENTER);sample.setPadding(0,dp(18),0,dp(10));sample.setLineSpacing(dp(6),1.1f);box.addView(sample);
    size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){sample.setTextSize(p+14);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
    new AlertDialog.Builder(this).setTitle("Reading settings").setView(box).setNegativeButton("Cancel",null).setPositiveButton("Apply",(d,w)->{
      savePosition();theme=options.getCheckedRadioButtonId()-100;font=size.getProgress()+24;prefs.edit().putInt("theme",theme).putInt("font",font).apply();
      if(chapter>=0)renderReader(prefs.getInt("lastVerse",0),prefs.getInt("lastOffset",0));else home(tab);
    }).show();
  }
  void menu(){
    if(loading)return;
    if(chapter>=0){
      new AlertDialog.Builder(this).setItems(new String[]{"Previous surah","Next surah","Finish surah","About the text"},(d,w)->{
        if(w==0){if(chapter>0)open(chapter-1,0,0);else Toast.makeText(this,"This is the first surah",Toast.LENGTH_SHORT).show();}
        if(w==1){if(chapter<113)open(chapter+1,0,0);else Toast.makeText(this,"This is the last surah",Toast.LENGTH_SHORT).show();}
        if(w==2)confirmFinishSurah();if(w==3)about();
      }).show();
    }else new AlertDialog.Builder(this).setItems(new String[]{"About the text","Reset reading progress"},(d,w)->{if(w==0)about();else new AlertDialog.Builder(this).setTitle("Reset reading progress?").setMessage("This clears verses read. Your bookmarks and reading position are kept.").setNegativeButton("Cancel",null).setPositiveButton("Reset",(x,y)->{SharedPreferences.Editor edit=prefs.edit();for(int i=0;i<114;i++){read[i]=0;viewed[i].clear();edit.remove("read"+i).remove("viewed"+i);}edit.apply();home(2);}).show();}).show();
  }
  void about(){
    new AlertDialog.Builder(this).setTitle("Quran").setMessage("114 surahs · 6,236 verses\n\nArabic: Uthmani text, Tanzil Project (tanzil.net), CC BY 3.0. Text is reproduced without alteration.\n\nEnglish: Saheeh International (en.sahih).\nHindi: Suhel Farooq Khan and Saifur Rahman Nadwi (hi.hindi).\n\nText supplied by AlQuran Cloud (alquran.cloud). Translations convey interpretations of the meanings.\n\nAll text is included offline. No account, ads, analytics or internet permission. Progress and bookmarks stay on this device. Hold a verse to save or copy it. Use the bottom Mark read button to confirm visible verses, or Finish surah from the menu. Progress is saved on this device after confirmation.").setPositiveButton("Close",null).show();
  }
  void back(){if(chapter>=0)home(0);else if(tab!=0)home(0);else finish();}
  @Override public void onBackPressed(){back();}
  @Override protected void onPause(){savePosition();super.onPause();}
  @Override protected void onDestroy(){destroyed=true;request++;worker.shutdown();super.onDestroy();}
  @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);savePosition();b.putInt("chapter",chapter);b.putInt("tab",tab);b.putInt("verse",prefs.getInt("lastVerse",0));b.putInt("offset",prefs.getInt("lastOffset",0));}
}

