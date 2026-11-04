package app.sukoon.quran;
import org.robolectric.annotation.*;
@Config(sdk={26,27,28,29,30,31,32,33,34,35,36})
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class Android8To16CompatibilityTest extends ProgressRegressionTest {
 @Override @org.junit.Test public void captureEndState()throws Exception{
  legacyFive();open(0,6);capture(a.root,"reader-api-"+android.os.Build.VERSION.SDK_INT);
  a.markButton.performClick();save();a.home(2);layout();
  org.junit.Assert.assertEquals(7,a.read[0]);org.junit.Assert.assertEquals(1,a.completed());
 }
}
