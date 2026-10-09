# jarrunner/jextract

A fork of [openjdk/jextract](https://github.com/openjdk/jextract), kept for [jarrunner/jr](https://github.com/jarrunner/jr). It carries jextract's own code unchanged except for the commits below, and adds one directory, `teavm/`: a generator of TeaVM `@Import` bindings from C headers, built on jextract's parser (see `teavm/README.md`).

## Branch `teavm`, on top of upstream `master`

1. `Wide-string macros: decode the full literal, ...`: a fix to jextract itself (wide and Unicode string macros were truncated to their first code unit), with a jtreg test. Meant for upstream.
2. `module-info: export and open the impl package to jarrunner.jextract_teavm only`: lets the generator module use jextract's parser without `--add-opens`.
3. The `teavm/` directory and one `apply from` line in `build.gradle` that builds it, plus the workflow `.github/workflows/jarrunner-teavm-image.yml`.

To follow upstream: rebase `teavm` onto upstream `master`, drop commit 1 once upstream has the fix, and rebuild.

## Licences

jextract is GPL v2 with the Classpath Exception (`LICENSE`). `teavm/` is the Apache License 2.0 (`teavm/LICENSE`) and is a separate module that uses jextract through its module interface; the two are only linked, which is what the Classpath Exception allows.

## Releases

Tags `teavm-v*` build runtime images (jextract + the generator + libclang) for Linux x64 and Windows x64 and attach them to a GitHub release.
