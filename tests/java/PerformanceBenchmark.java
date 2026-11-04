package app.sukoon.quran;
import android.app.AlertDialog;
import android.os.Looper;
import android.view.View;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=36,qualifiers="w360dp-h760dp-xxhdpi")
public class PerformanceBenchmark {
 MainActivity a;ActivityController<MainActivity> ctl;
 static final int WARMUPS=5,SAMPLES=30;
 void create(){ctl=Robolectric.buildActivity(MainActivity.class).setup();a=ctl.get();layout();}
 void close(){ctl.pause().stop().destroy();Shadows.shadowOf(Looper.getMainLooper()).idle();}
 void layout(){a.root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2200,View.MeasureSpec.EXACTLY));a.root.layout(0,0,1080,2200);}
 void settle()throws Exception{a.worker.submit(()->{}).get(20,TimeUnit.SECONDS);Shadows.shadowOf(Looper.getMainLooper()).idle();layout();}
 double elapsed(long start){return (System.nanoTime()-start)/1_000_000.0;}
 JSONObject result(String id,String description,List<Double> samples)throws Exception{
  List<Double> sorted=new ArrayList<>(samples);Collections.sort(sorted);
  return new JSONObject().put("id",id).put("description",description).put("samples",new JSONArray(samples))
    .put("n",samples.size()).put("median_ms",(sorted.get(14)+sorted.get(15))/2)
    .put("p95_ms",sorted.get((int)Math.ceil(sorted.size()*.95)-1)).put("min_ms",sorted.get(0)).put("max_ms",sorted.get(sorted.size()-1));
 }
 @Test public void measureHostHarness()throws Exception{
  RuntimeEnvironment.getApplication().getSharedPreferences("reader2",0).edit().clear().commit();
  JSONArray operations=new JSONArray();List<Double> samples=new ArrayList<>();
  for(int i=-WARMUPS;i<SAMPLES;i++){long start=System.nanoTime();create();double ms=elapsed(start);if(i>=0)samples.add(ms);close();}
  operations.put(result("activity","Fresh Activity create and first explicit layout, warm JVM",samples));
  create();samples=new ArrayList<>();
  for(int i=-WARMUPS;i<SAMPLES;i++){
   a.worker.submit(()->{a.cache.clear();a.packedGroup=-1;a.packedChapters=null;}).get();
   long start=System.nanoTime();a.open(0,0,0);settle();double ms=elapsed(start);if(i>=0)samples.add(ms);
   org.junit.Assert.assertEquals(7,a.ayahs.length());
  }
  operations.put(result("reader_cold","Open Al-Faatiha with a new XZ group, worker and layout included",samples));
  samples=new ArrayList<>();
  for(int i=-WARMUPS;i<SAMPLES;i++){long start=System.nanoTime();a.open(0,0,0);settle();double ms=elapsed(start);if(i>=0)samples.add(ms);}
  operations.put(result("reader_cached","Open cached Al-Faatiha, worker and layout included",samples));
  samples=new ArrayList<>();
  for(int i=-WARMUPS;i<SAMPLES;i++){
   AlertDialog d=new AlertDialog.Builder(a).setPositiveButton("Save",null).create();d.show();
   BitSet selection=new BitSet();if(i%2==0)selection.set(6);
   long start=System.nanoTime();a.saveReadProgress(0,selection,d);settle();double ms=elapsed(start);if(i>=0)samples.add(ms);
  }
  operations.put(result("save","Changed progress commit, main-thread callback and explicit layout",samples));close();
  String cpu="not recorded";Path cpuInfo=Paths.get("/proc/cpuinfo");
  if(Files.exists(cpuInfo))for(String line:Files.readAllLines(cpuInfo))if(line.startsWith("model name")){cpu=line.substring(line.indexOf(':')+1).trim();break;}
  JSONObject output=new JSONObject().put("environment","Robolectric 4.17, Android API 36, native graphics, Linux cloud VM")
    .put("physical_device",false).put("warmup_iterations",WARMUPS).put("samples_per_operation",SAMPLES)
    .put("java",System.getProperty("java.version")).put("cpu",cpu).put("available_processors",Runtime.getRuntime().availableProcessors())
    .put("viewport_pixels","1080 x 2200").put("qualifiers","w360dp-h760dp-xxhdpi")
    .put("limitations","Host wall-clock harness timings; warm JVM and host file cache. Not Android process startup, human tap latency, FPS, RAM usage, or physical-device performance.")
    .put("operations",operations);
  Path target=Paths.get(System.getProperty("quran.benchmarks","build/test-results/benchmarks.json"));
  Files.createDirectories(target.getParent());Files.write(target,output.toString(2).getBytes(StandardCharsets.UTF_8));
 }
}
