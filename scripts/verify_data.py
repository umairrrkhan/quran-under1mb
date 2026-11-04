from pathlib import Path
import hashlib,json,lzma,struct
ROOT=Path(__file__).resolve().parents[1]
def chapters():
 result=[]
 for group in range(3):
  raw=lzma.decompress((ROOT/'data'/f'group{group}.xz').read_bytes());offset=0
  for _ in range(min(40,114-group*40)):
   length=struct.unpack_from('>I',raw,offset)[0];offset+=4
   result.append(json.loads(raw[offset:offset+length]));offset+=length
  assert offset==len(raw),'Unexpected trailing bytes'
 return result
def verify():
 manifest=json.loads((ROOT/'data/checksums.json').read_text())
 for name,expected in manifest.items():
  raw=(ROOT/'data'/name).read_bytes()
  assert len(raw)==expected['bytes'] and hashlib.sha256(raw).hexdigest()==expected['sha256'],name
 index=json.loads((ROOT/'data/index.json').read_text(encoding='utf-8'));all_chapters=chapters()
 assert len(index)==len(all_chapters)==114
 for meta,verses in zip(index,all_chapters):
  assert len(verses)==meta['count']
  for verse in verses:assert all(isinstance(verse[k],str) and verse[k].strip() for k in ['a','e','h'])
 assert sum(map(len,all_chapters))==6236
 apk=ROOT/'downloads/Quran.apk'
 expected=(ROOT/'downloads/SHA256SUMS').read_text().split()[0]
 assert hashlib.sha256(apk.read_bytes()).hexdigest()==expected
 assert apk.stat().st_size<1_000_000
 print('Verified 114 surahs, 6236 Arabic/English/Hindi verse records, asset hashes and release APK checksum.')
if __name__=='__main__':verify()
