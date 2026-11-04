from pathlib import Path
import argparse,os,subprocess,zipfile
from build_android import ROOT,BUILD,tools,run
def prepare():
 platform,bt=tools();deps=BUILD/'test-deps';deps.mkdir(parents=True,exist_ok=True)
 run(['mvn','-q','-f',ROOT/'tests/pom.xml','dependency:copy-dependencies','-DoutputDirectory='+str(deps)])
 for p in deps.glob('*.aar'):
  with zipfile.ZipFile(p) as z:p.with_suffix('.jar').write_bytes(z.read('classes.jar'))
 classes=BUILD/'classes';cfg=classes/'com/android/tools/test_config.properties';cfg.parent.mkdir(parents=True,exist_ok=True)
 values={'android_resource_apk':BUILD/'unsigned.apk','android_merged_manifest':ROOT/'android/AndroidManifest.xml','android_custom_package':'app.sukoon.quran','android_merged_assets':ROOT/'data','android_merged_resources':ROOT/'android/res'}
 cfg.write_text(''.join(k+'='+str(v).replace('\\','/')+'\n' for k,v in values.items()))
 (BUILD/'screenshots').mkdir(exist_ok=True);(BUILD/'test-results').mkdir(exist_ok=True)
 cp=os.pathsep.join(map(str,[platform,str(deps/'*'),classes,BUILD/'xz-1.10.jar']))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',classes,*sorted((ROOT/'tests/java').glob('*.java'))])
 return os.pathsep.join(map(str,[classes,str(deps/'*'),BUILD/'xz-1.10.jar',platform]))
def execute(names,log):
 cp=prepare()
 command=['java','--add-opens=java.base/jdk.internal.access=ALL-UNNAMED','--add-opens=java.base/java.io=ALL-UNNAMED','--add-opens=java.base/java.lang=ALL-UNNAMED','-Xmx5000m','-Drobolectric.dependency.repo.url=https://repo.maven.apache.org/maven2','-Drobolectric.logging.enabled=true','-Dquran.screenshots='+str(BUILD/'screenshots'),'-Dquran.benchmarks='+str(BUILD/'test-results/benchmarks.json'),'-cp',cp,'org.junit.runner.JUnitCore',*names]
 with open(log,'w') as output:result=subprocess.run(command,cwd=ROOT,stdout=output,stderr=subprocess.STDOUT)
 print(Path(log).read_text(errors='replace')[-10000:]);return result.returncode
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--matrix',action='store_true');parser.add_argument('--benchmark',action='store_true');args=parser.parse_args()
 names=['app.sukoon.quran.PerformanceBenchmark'] if args.benchmark else ['app.sukoon.quran.Android7CompatibilityTest','app.sukoon.quran.Android8To16CompatibilityTest'] if args.matrix else ['app.sukoon.quran.ProgressRegressionTest']
 BUILD.mkdir(exist_ok=True);(BUILD/'test-results').mkdir(exist_ok=True)
 raise SystemExit(execute(names,BUILD/'test-results'/('benchmark.log' if args.benchmark else 'matrix.log' if args.matrix else 'regression.log')))
if __name__=='__main__':main()
