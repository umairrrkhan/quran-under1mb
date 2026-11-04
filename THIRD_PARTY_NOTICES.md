# Data and third-party notices

The code license does not change the terms of bundled text or libraries.

## Arabic Quran

Tanzil Quran Text. Copyright (C) 2007–2021 Tanzil Project. License: Creative Commons Attribution 3.0.

The Uthmani text is reproduced unchanged. Tanzil requires attribution, a link to its site, preservation of its notice, and verbatim reproduction. See the [full text license](https://tanzil.net/docs/text_license), [Tanzil](https://tanzil.net) and [updates](https://tanzil.net/updates/).

## Translations

- English: **Saheeh International**, edition `en.sahih`.
- Hindi: **Suhel Farooq Khan and Saifur Rahman Nadwi**, edition `hi.hindi`.

Editions were obtained through [AlQuran Cloud](https://alquran.cloud/api) and retain their original rights. Its [terms](https://alquran.cloud/terms-and-conditions) require translator attribution when republishing. [Tanzil's translation terms](https://tanzil.net/trans/) provide translations for non-commercial use and require translator/publisher permission for other uses. This repository does not relicense translations under MIT or assert they are public domain. Retain attribution when sharing this free educational reader.

## XZ for Java

The released APK uses **org.tukaani:xz 1.10**, under the **BSD Zero Clause License (0BSD)**. See [upstream licensing](https://tukaani.org/xz/java.html#licensing). Build and benchmark tooling pins the version so the published implementation can be studied. Upstream has newer releases; upgrading requires rechecking corpus decoding, package size and compatibility.

## Build and test tools

Android SDK/build tools, OpenJDK, Python, Maven, JUnit and Robolectric are build/test dependencies with their own licenses. They are not bundled into the APK. Android system fonts are used; no font files or audio are bundled.
