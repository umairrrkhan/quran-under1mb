# Physical-device benchmark protocol

No handset latency measurements have been collected. Robolectric host timings are not substitutes for device measurements.

Record the phone model, Android version, RAM, refresh rate, power mode, thermal state, app version, font size and reading language. Keep raw samples and use the same chapter/verse.

## Cold and warm launch

With platform-tools and a connected test phone, collect at least ten runs:

    adb shell am force-stop app.sukoon.quran
    adb shell am start -W -n app.sukoon.quran/.MainActivity

Record ThisTime, TotalTime and WaitTime where supplied by the OS. This measures Activity launch, not necessarily the moment all text is drawn. For a warm comparison, return home and reopen without force-stopping. Document the conditions.

## Reading and scrolling

Measure tap-to-usable-text with screen recording or instrumentation. Include a new compressed group, a cached chapter, the longest verses, the largest font and Arabic plus translation.

    adb shell dumpsys gfxinfo app.sukoon.quran reset
    # Perform the same scrolling sequence.
    adb shell dumpsys gfxinfo app.sukoon.quran framestats
    adb shell dumpsys meminfo app.sukoon.quran

Report median/p95 frame time, janky frames, memory, sample count and the sequence. A 60 Hz interval is about 16.7 ms; 120 Hz is about 8.3 ms. This project does not claim consistent compliance without device measurements.

## Saving

Start at 5/7 in Al-Faatiha. Scroll to the end, tap Mark read, confirm verses 6 and 7, then Save. Wait for Saved, close/reopen and check 7/7. Repeat with Finish surah, cancellation, and a save followed immediately by Activity closure.
