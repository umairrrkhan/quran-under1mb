"""Build with an existing Android SDK; installs no SDK automatically."""
from pathlib import Path
import hashlib,os,subprocess,urllib.request,zipfile
ROOT=Path(__file__).resolve().parents[1];BUILD=ROOT/'build'
def run(args,**kwargs):subprocess.run([str(x) for x in args],check=True,**kwargs)
def tools():
 sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))
 platform=Path(os.environ.get('QURAN_ANDROID_JAR',str(sdk/'platforms/android-36/android.jar')))
 bt=Path(os.environ.get('QURAN_BUILD_TOOLS',str(sdk/'build-tools/36.0.0')))
 if not platform.is_file() or not (bt/('aapt2.exe' if os.name=='nt' else 'aapt2')).is_file():
  raise SystemExit('Set ANDROID_HOME to an SDK with platforms;android-36 and build-tools;36.0.0. This script installs no SDK.')
 return platform,bt
def main():
 BUILD.mkdir(exist_ok=True);platform,bt=tools();classes=BUILD/'classes';classes.mkdir(exist_ok=True)
 generated=BUILD/'generated';generated.mkdir(exist_ok=True);dex=BUILD/'dex';dex.mkdir(exist_ok=True)
 xz=BUILD/'xz-1.10.jar'
 if not xz.exists():urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/org/tukaani/xz/1.10/xz-1.10.jar',xz)
 expected=(ROOT/'scripts/xz-1.10.sha256').read_text().strip()
 assert hashlib.sha256(xz.read_bytes()).hexdigest()==expected,'XZ dependency checksum mismatch'
 def tool(name):return bt/(name+('.bat' if name in ['d8','apksigner'] else '.exe') if os.name=='nt' else name)
 run([tool('aapt2'),'compile','--dir',ROOT/'android/res','-o',BUILD/'resources.zip'])
 run([tool('aapt2'),'link','-o',BUILD/'unsigned.apk','-I',platform,'--manifest',ROOT/'android/AndroidManifest.xml','-A',ROOT/'data','-0','xz','--java',generated,BUILD/'resources.zip'])
 boot=os.pathsep.join(map(str,[bt/'core-lambda-stubs.jar',platform]))
 sources=list((ROOT/'android/src').rglob('*.java'))+list(generated.rglob('*.java'))
 run(['javac','-encoding','UTF-8','-source','8','-target','8','-bootclasspath',boot,'-cp',xz,'-d',classes,*sources])
 run([tool('d8'),'--lib',platform,'--min-api','24','--output',dex,*classes.rglob('MainActivity*.class'),xz])
 with zipfile.ZipFile(BUILD/'unsigned.apk','a',zipfile.ZIP_DEFLATED,compresslevel=9) as z:z.write(dex/'classes.dex','classes.dex')
 run([tool('zipalign'),'-f','4',BUILD/'unsigned.apk',BUILD/'aligned.apk'])
 key=Path(os.environ.get('QURAN_KEYSTORE',str(BUILD/'development.p12')))
 password=os.environ.get('QURAN_STORE_PASSWORD','development-only');alias=os.environ.get('QURAN_KEY_ALIAS','quran')
 if not key.exists():
  if os.environ.get('QURAN_KEYSTORE'):raise SystemExit('Configured signing key does not exist')
  run(['keytool','-genkeypair','-keystore',key,'-storepass',password,'-keypass',password,'-alias',alias,'-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Quran local development'])
 run([tool('apksigner'),'sign','--ks',key,'--ks-key-alias',alias,'--ks-pass','pass:'+password,'--out',BUILD/'Quran-development.apk',BUILD/'aligned.apk'])
 run([tool('apksigner'),'verify','--min-sdk-version','24','--verbose',BUILD/'Quran-development.apk'])
 assert (BUILD/'Quran-development.apk').stat().st_size<1_000_000,'APK exceeded the size budget'
 print('Built',BUILD/'Quran-development.apk','bytes:',(BUILD/'Quran-development.apk').stat().st_size)
if __name__=='__main__':main()
