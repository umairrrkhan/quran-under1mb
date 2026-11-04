package app.sukoon.quran;
import org.junit.Test;
import org.robolectric.annotation.*;
@Config(sdk={24,25})
@GraphicsMode(GraphicsMode.Mode.LEGACY)
public class Android7CompatibilityTest extends ProgressRegressionTest {
 @Override @Test public void captureEndState()throws Exception{
  legacyFive();open(0,6);a.markButton.performClick();save();a.home(2);layout();
  org.junit.Assert.assertEquals(7,a.read[0]);org.junit.Assert.assertEquals(1,a.completed());
 }
}
