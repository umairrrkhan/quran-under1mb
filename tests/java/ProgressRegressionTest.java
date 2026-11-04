package app.sukoon.quran;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import android.app.*;import android.widget.*;import android.view.*;import android.os.*;
import java.util.concurrent.TimeUnit;
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=36,qualifiers="w360dp-h760dp-xxhdpi")
public class ProgressRegressionTest {
 ActivityController<MainActivity> ctl;MainActivity a;
 @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("reader2",0).edit().clear().commit();ctl=Robolectric.buildActivity(MainActivity.class).setup();a=ctl.get();layout();}
 void settle()throws Exception{a.worker.submit(()->{}).get(20,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();layout();}
 void layout(){a.root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2200,View.MeasureSpec.EXACTLY));a.root.layout(0,0,1080,2200);}
 void open(int c,int v)throws Exception{a.open(c,v,0);settle();}
 AlertDialog dialog(){return org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();}
 void save()throws Exception{dialog().getButton(-1).performClick();settle();}
 void exact(int n)throws Exception{a.chooseReadNumber();find(dialog().getWindow().getDecorView()).setText(""+n);save();}
 void legacyFive(){a.prefs.edit().putInt("read0",5).putString("viewed0","0,1,2,3,4").commit();ctl.recreate();a=ctl.get();layout();}
 @After public void stop(){ctl.pause().stop().destroy();}
 @Test public void finalTwoCompleteAndSurviveReopening()throws Exception{
  legacyFive();open(0,6);assertEquals(6,a.visibleVerses()[1]);assertEquals(5,a.read[0]);a.markButton.performClick();
  String labels="";for(int n=0;n<dialog().getListView().getAdapter().getCount();n++)labels+=dialog().getListView().getAdapter().getItem(n)+" ";assertTrue(labels.contains("Verse 6"));assertTrue(labels.contains("Verse 7"));save();
  assertEquals(7,a.read[0]);assertEquals(1,a.completed());assertTrue(a.positionLabel.getText().toString().contains("Read: 7 / 7"));
  java.io.File file=new java.io.File(a.getApplicationInfo().dataDir,"shared_prefs/reader2.xml");assertTrue("Saved must mean written to disk",file.isFile());
  String disk=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);assertTrue(disk.contains("0,1,2,3,4,5,6"));
  ctl.recreate();a=ctl.get();settle();assertEquals(7,a.read[0]);assertEquals(1,a.completed());
 }
 @Test public void finalVerseNeedsNoEighthVerse()throws Exception{open(0,6);exact(7);assertEquals(1,a.read[0]);assertTrue(a.viewed[0].get(6));assertFalse(a.viewed[0].get(5));exact(6);assertEquals(2,a.read[0]);exact(7);assertEquals(2,a.read[0]);}
 @Test public void finishAtBottomExplicitlyCompletesSurah()throws Exception{legacyFive();open(0,6);a.markButton.performClick();assertEquals("Finish surah",dialog().getButton(-3).getText().toString());dialog().getButton(-3).performClick();assertEquals(5,a.read[0]);save();assertEquals(7,a.read[0]);assertEquals(1,a.completed());}
 @Test public void cancelLeavesProgressUntouched()throws Exception{legacyFive();open(0,6);a.markButton.performClick();dialog().getButton(-2).performClick();settle();assertEquals(5,a.read[0]);assertFalse(a.viewed[0].get(5));}
 @Test public void scrollingDoesNotInventCompletion()throws Exception{open(0,0);a.list.setSelection(6);layout();Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));assertEquals(0,a.totalRead());a.home(2);a.resume();settle();assertEquals(0,a.totalRead());}
 @Test public void duplicateSelectionDoesNotDoubleCount()throws Exception{open(0,6);a.markButton.performClick();save();int n=a.read[0];a.markButton.performClick();save();assertEquals(n,a.read[0]);assertEquals(n,a.totalRead());}
 @Test public void firstAndLastVersesAcrossSurahsSave()throws Exception{for(int c:new int[]{0,1,107,113}){open(c,0);exact(1);exact(a.count(c));assertTrue(a.viewed[c].get(0));assertTrue(a.viewed[c].get(a.count(c)-1));assertEquals(2,a.read[c]);}}
 @Test public void selectionDoesNotChangeWhenListMoves()throws Exception{open(1,254);int[] range=a.visibleVerses();a.markButton.performClick();a.list.setSelectionFromTop(0,0);layout();save();assertFalse(a.viewed[1].get(0));for(int n=range[0];n<=range[1];n++)assertTrue(a.viewed[1].get(n));}
 @Test public void invalidVerseCannotChangeProgress()throws Exception{open(0,0);a.chooseReadNumber();EditText input=find(dialog().getWindow().getDecorView());input.setText("8");dialog().getButton(-1).performClick();assertNotNull(input.getError());assertTrue(dialog().isShowing());assertEquals(0,a.totalRead());}
 @Test public void bookmarksAndPositionStayIndependent()throws Exception{open(1,254);a.toggleBookmark(254);a.savePosition();assertEquals(0,a.totalRead());a.home(1);settle();assertEquals(1,a.adapter.getCount());a.resume();settle();assertEquals(1,a.chapter);assertEquals(254,a.currentVerse());assertEquals(0,a.totalRead());}
 @Test public void languagesAndLargeTextKeepFooter()throws Exception{open(0,0);LinearLayout row=(LinearLayout)a.adapter.getView(0,null,a.list);assertTrue(((TextView)row.getChildAt(2)).getText().toString().contains("Allah"));a.language=1;row=(LinearLayout)a.adapter.getView(0,row,a.list);assertTrue(((TextView)row.getChildAt(2)).getText().toString().contains("अल्लाह"));a.language=2;row=(LinearLayout)a.adapter.getView(0,row,a.list);assertEquals(View.GONE,row.getChildAt(2).getVisibility());a.font=42;a.renderReader(0,0);layout();assertTrue(a.positionLabel.getText().length()>0);assertEquals("Mark read",a.markButton.getText().toString());}
 @Test public void freshActivityRestoresExactSavedIds()throws Exception{open(0,0);exact(7);a.toggleBookmark(0);ctl.pause().stop().destroy();ctl=Robolectric.buildActivity(MainActivity.class).setup();a=ctl.get();layout();assertEquals(1,a.read[0]);assertTrue(a.viewed[0].get(6));assertTrue(a.bookmarks.contains("0:0"));assertEquals("6",a.prefs.getString("viewed0",null));}
 @Test public void counterIncludesBothFinalVerses()throws Exception{open(0,6);int[] range=a.visibleVerses();assertEquals(6,range[1]);assertTrue(range[0]<=5);assertTrue(a.positionLabel.getText().toString().contains("of 7"));a.language=3;open(1,254);LinearLayout row=(LinearLayout)a.list.getChildAt(0);a.list.scrollListBy(row.getBottom()-a.dp(1));layout();assertTrue(a.visibleVerses()[0]>254);}
 @Test public void pendingSaveFinishesAfterActivityCloses()throws Exception{open(0,6);a.chooseReadNumber();find(dialog().getWindow().getDecorView()).setText("7");dialog().getButton(-1).performClick();ctl.pause().stop().destroy();assertTrue(a.worker.awaitTermination(10,TimeUnit.SECONDS));Shadows.shadowOf(Looper.getMainLooper()).idle();ctl=Robolectric.buildActivity(MainActivity.class).setup();a=ctl.get();layout();assertTrue(a.viewed[0].get(6));assertEquals(1,a.read[0]);}
 @Test public void captureEndState()throws Exception{legacyFive();open(0,6);capture(a.root,"reader-end");a.markButton.performClick();View v=dialog().getWindow().getDecorView();v.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.AT_MOST));v.layout(0,0,1000,v.getMeasuredHeight());capture(v,"confirm-end");save();capture(a.root,"reader-saved");a.home(2);layout();capture(a.root,"progress-saved");}
 String text(View v){String s=v instanceof TextView?((TextView)v).getText().toString()+" ":"";if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)s+=text(g.getChildAt(i));}return s;}
 EditText find(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){EditText e=find(g.getChildAt(i));if(e!=null)return e;}}return null;}
 void capture(View v,String n)throws Exception{android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(v.getWidth(),v.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(b));b=android.graphics.Bitmap.createScaledBitmap(b,432,Math.max(1,b.getHeight()*432/b.getWidth()),true);try(java.io.FileOutputStream out=new java.io.FileOutputStream(System.getProperty("quran.screenshots","build/screenshots")+"/"+n+".webp")){b.compress(android.graphics.Bitmap.CompressFormat.WEBP,80,out);}}
}
