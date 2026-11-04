# Quran under 1 MB

**Android 7–16 · Arabic, English and Hindi · fully offline · 881,355-byte APK**

## Response time and size

**Physical-phone startup/tap latency has not been measured.** These are Linux cloud **Robolectric framework-harness** timings with a warm JVM and host file cache. They are not handset latency, FPS or a zero-lag guarantee.

| Host harness operation | Median ms | p95 ms |
|---|---:|---:|
| Activity creation and layout | 132.855 | 186.431 |
| New-group reader opening | 126.567 | 174.341 |
| Cached reader opening | 32.671 | 34.779 |
| Progress commit and UI callback | 35.413 | 48.831 |

Five warm-ups and thirty measured samples per operation. Robolectric 4.17, Android API 36, native graphics, Linux cloud VM; Java 21.0.12.1; 4 available processors; Intel(R) Xeon(R) Processor @ 2.50GHz.

[Raw timings](docs/benchmarks.json) · [CSV samples](docs/benchmark-samples.csv) · [Device protocol](docs/DEVICE_BENCHMARK.md) · **[Report PDF](docs/report.pdf)** · [LaTeX source](docs/report.tex)

| Verified property | Result |
|---|---|
| Signed APK | **881,355 bytes = 0.881355 MB = 860.7 KiB** |
| Strict cap | Less than **1,000,000 bytes**; 118,645 bytes of headroom |
| Android support | **7.0 through 16**, including 7.1, 8.1 and 12L |
| Framework validation | **195 checks passed**: 15 cases on each API 24–36 |
| Offline corpus | 114 surahs; 6,236 aligned verses in each language |
| Runtime permissions | None |

## Download and install

**[Download Quran.apk](https://github.com/umairrrkhan/quran-under1mb/raw/refs/heads/main/downloads/Quran.apk)** · [SHA-256 checksum](downloads/SHA256SUMS)

1. Download on an Android 7+ phone.
2. Open the APK in your browser or file manager.
3. If Android asks, allow that browser/file manager to install apps, then tap **Install**.
4. Open **Quran**. No account or connection is needed.

Install a maintainer-signed update over the previous maintainer-signed build to keep data. Uninstalling or clearing data removes progress/bookmarks. A source build with a different key cannot replace the release in place.

## Reading and progress

Search surahs or resume your position. Choose Arabic + English, Arabic + Hindi, or one language alone. Use **Aa** for text size and system/light/dark appearance. Hold a verse to bookmark or copy it.

The bottom counter shows text currently visible; **Read** shows saved completion. Tap **Mark read**, confirm verse numbers, then **Save**. **Finish surah** explicitly completes a surah, including its final verses. Scrolling and timers do not claim you read something. Saved is shown after the write succeeds.

## Why it is small

Native Java views, recycled ListView rows and system fonts avoid heavy UI dependencies. Startup reads a small index. A worker decodes the requested XZ group; one decoded group and up to three parsed chapters are retained. No audio, AI model, web framework, advertising SDK or analytics is bundled.

Compressed text occupies **775,704 bytes (88.01% of the APK)**. A whole-corpus experiment saves **46,396 payload bytes**, about **5.26%** of this APK with other costs fixed, but increases decode scope and retained data. It is not implemented. [Size analysis](docs/size-analysis.json) and the report explain the tradeoff.

## Build from source

Requirements: **OpenJDK 21**, **Python 3**, Android **platform 36** and **build-tools 36.0.0**. Tests also use Maven. Android Studio is not required. End users need only the APK.

    export ANDROID_HOME=/path/to/android-sdk
    python3 scripts/verify_data.py
    python3 scripts/build_android.py

Output: **build/Quran-development.apk**. The script downloads pinned XZ 1.10 and verifies its hash; it installs no SDK. It creates a local development key in ignored build/ unless QURAN_KEYSTORE, QURAN_STORE_PASSWORD and QURAN_KEY_ALIAS are supplied. Keep keys private and reuse the same key for updates.

    python3 scripts/run_tests.py
    python3 scripts/run_tests.py --matrix
    python3 scripts/run_tests.py --benchmark
    python3 scripts/analyze_size.py

The default test command runs Android 16 regressions; --matrix covers API 24–36. Tests use Android framework classes in Robolectric. Android 7/7.1 use legacy harness graphics; Android 8–16 use native graphics. Physical devices, full OS emulators, every manufacturer skin, battery use, RAM and frame rates were not tested.

## Reproduce the report

Copy fresh build/test-results/benchmarks.json into docs/, then run:

    python3 scripts/render_docs.py
    cd docs
    pdflatex -interaction=nonstopmode -halt-on-error report.tex

The release hash is in downloads/SHA256SUMS. Source builds may differ in signatures, signing padding and auxiliary notices; bit-identical reproduction is not claimed.

## Source layout

- **android/** — app, manifest and resources
- **data/** — index, XZ groups and checksums
- **downloads/** — signed APK and checksum
- **tests/** — regression, compatibility and benchmark code
- **scripts/** — builds, verification and report tooling
- **docs/** — LaTeX/PDF report, measurements and size analysis

## Openness and attribution

Application code, scripts and documentation are **MIT licensed**. Quran text and translations retain upstream terms and are **not** relicensed as MIT. Arabic text is attributed to Tanzil; English is Saheeh International; Hindi is by Suhel Farooq Khan and Saifur Rahman Nadwi. Data came through AlQuran Cloud. Translation terms include non-commercial-use conditions. See [third-party notices](THIRD_PARTY_NOTICES.md).

Private signing material is not in this repository. Contributions should preserve verse strings, pass regressions and keep the APK below the strict cap.
