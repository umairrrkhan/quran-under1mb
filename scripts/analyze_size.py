from pathlib import Path
import json,hashlib,lzma,struct,zipfile
from verify_data import ROOT,chapters
def analyze():
 apk=ROOT/'downloads/Quran.apk';size=apk.stat().st_size
 with zipfile.ZipFile(apk) as z:
  entries=[{'name':i.filename,'stored_bytes':i.compress_size,'decoded_bytes':i.file_size} for i in z.infolist()]
  groups=[z.read('assets/group'+str(n)+'.xz') for n in range(3)]
  index=z.read('assets/index.json')
 corpus=chapters();raw=[json.dumps(x,ensure_ascii=False,separators=(',',':')).encode() for x in corpus]
 experiments=[]
 for n in [1,10,20,40,114]:
  sizes=[];decoded=[]
  for start in range(0,114,n):
   payload=b''.join(struct.pack('>I',len(x))+x for x in raw[start:start+n])
   sizes.append(len(lzma.compress(payload,preset=6)));decoded.append(len(payload))
  experiments.append({'chapters_per_group':n,'groups':len(sizes),'compressed_payload_bytes':sum(sizes),'largest_decoded_group_bytes':max(decoded)})
 current=sum(map(len,groups))
 result={'apk_bytes':size,'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'apk_mb_decimal':size/1_000_000,'apk_kib':size/1024,
  'budget_bytes':1_000_000,'budget_headroom_bytes':1_000_000-size,'surahs':114,'verses_per_language':6236,'languages':3,
  'compressed_text_bytes':current,'compressed_text_share_percent':100*current/size,
  'index_bytes':len(index),'decoded_text_bytes':sum(len(lzma.decompress(x)) for x in groups),
  'entries':entries,'compression_experiments':experiments,
  'whole_corpus_payload_saving_bytes':current-experiments[-1]['compressed_payload_bytes'],
  'whole_corpus_package_saving_percent_estimate':100*(current-experiments[-1]['compressed_payload_bytes'])/size,
  'experiment_limitations':'Payload sizes measured with Python XZ preset 6. Whole-APK estimates keep all other bytes fixed and ignore changed decoder/cache logic, signing and alignment. Larger groups increase decode scope and retained text.'}
 dest=ROOT/'docs/size-analysis.json';dest.parent.mkdir(exist_ok=True);dest.write_text(json.dumps(result,indent=2)+'\n')
 print(json.dumps({k:v for k,v in result.items() if k not in ['entries','compression_experiments']},indent=2))
if __name__=='__main__':analyze()
