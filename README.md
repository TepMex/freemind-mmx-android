# FreeMind-MMX Android

This repository contains:

1. **Legacy FreeMind / FreeMind-MMX desktop sources** (`freemind/`, plus `pda/`, `flash/`, …) — preserved as the behavioral reference for the FreeMind `.mm` format and the FreeMind-MMX `.mmx` sidecar design.
2. **A new Android application** (`android/`) — a modern Kotlin / Jetpack Compose mind-mapping client that aims for FreeMind interoperability without porting Swing UI.

## Why FreeMind-MMX?

FreeMind-MMX (Jiang Xin / OSSXP) improves version-control friendliness:

- Writes UTF-8 text instead of numeric XML character entities for non-ASCII
- Moves volatile attributes (`FOLDED`, `CREATED`, `MODIFIED`) into a hidden `.<name>.mmx` sidecar so expanding/collapsing nodes does not dirty the main `.mm` file

See the original notes below and the archaeology report:
[`docs/android-port-analysis.md`](docs/android-port-analysis.md).

## Android application (current work)

| Item | Location |
|------|----------|
| App + modules | [`android/`](android/) |
| Build / test instructions | [`android/README.md`](android/README.md) |
| Architecture decisions | [`docs/adr/`](docs/adr/) |
| Port analysis | [`docs/android-port-analysis.md`](docs/android-port-analysis.md) |

**Current status:** Milestones 1–3 are in place (skeleton, `.mm`/`.mmx` reader, interactive mind-map viewer). Editor + writer next. CI publishes a signed sideload release APK.

```bash
cd android
./gradlew test
./gradlew :app:assembleDebug
```

## Legacy FreeMind-MMX (desktop)

The text below is retained from the upstream FreeMind-MMX README for historical context.

---

# FreeMind Hacking

FreeMind is an open source [mind-mapping](http://en.wikipedia.org/wiki/Mind_map)
software, and it's my favorite notebook. ;-)

## What is FreeMind?

References:

* [FreeMind Offical site](http://freemind.sourceforge.net/)
* [Online PPT from ossxp.com (in Chinese)](http://www.ossxp.com/HelpCenter/00000_OSSXP/AboutUs_Slide/1010%20开源小礼物)

## Why comes FreeMind-MMX?

Because it scratch my personal itch.

* FreeMind saves mind map in a XML file with extension ".mm".  Chinese
  characters in it are encoded as XML entities, such as "\&#xxx;", but
  *NOT* in UTF-8, which makes FreeMind document larger and hard to see
  differences between two revisions.

* All my documents are stored in a version control system (CVS and SVN
  before 2008, and Hg and Git latter).  But I find FreeMind's XML files
  are not version control friendly.  When I expand or collapse a node of
  mind map, FreeMind shows file has been modified and needs to be saved.
  This is because FreeMind records too many attributes for node including
  folded status, created time, modified time in the result XML file.
  Thus may make many unecessary commits to version control system.

I named my hacking of FreeMind as FreeMind-MMX, because when it saving
mind map, there are two files instead of one.  One output file (with extension
".mm") is a XML file, which store text contents and all necessary node
attributes, and another one is a hidden ".mmx" file, which is also a XML
file, but only stores auxiliary node attributes (such as folded, created
time and modified time).

## Build FreeMind-MMX from source (desktop)

Before build FreeMind-MMX from source code, you should install JDK.
Be sure to export a valid `JAVA_HOME`.

Then clone FreeMind-MMX from GitHub, and build.

1. Clone freemind-mmx repo:

        $ git clone git://github.com/jiangxin/freemind-mmx.git
        $ cd freemind-mmx

2. Checkout the master branch, and build

        $ git checkout master
        $ cd freemind
        $ ant dist
        $ ant post

3. Find build results in ../post directory, and click to install.

## Source / downloads (historical)

* [FreeMind-MMX on GitHub](https://github.com/jiangxin/freemind-mmx)
* [Download binaries from SourceForge](https://sourceforge.net/projects/freemind-mmx/files/FreeMind-MMX/)
