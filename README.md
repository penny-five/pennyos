# CleanYourTV — Philips 55POS9002/12

Personal checkout of [CleanYourTV](https://github.com/Gathin23/cleanyourtv) for a Philips
55POS9002/12 running Android TV 8.0. Keeps English and Finnish, with a reduced test suite.

- [TV notes and debloat list](examples/philips-55pos9002/README.md)
- [Launcher settings](launcher/app/src/main/res/values/config.xml) and [guide](docs/launcher.md)
- [Restore instructions](docs/restore.md)

Before changing the TV, follow [PROMPT.md](PROMPT.md): back up first, disable rather than
uninstall, confirm the replacement launcher is Home, and close ADB when finished.
The Philips notes cover the Netflix button remap and keeping the stock Source menu working.

## Local development

Requires Python 3, JDK 17, Android SDK platform 34 and build-tools 34.0.0.
Run from the repository root in PowerShell:

```powershell
python -m venv tools/venv
.	oolsenv\Scripts\python.exe -m pip install -r tools/requirements.txt
.	oolsenv\Scripts\python.exe -m unittest discover -s tests -v
.\launcher\gradlew.bat --no-daemon -p launcher testReleaseUnitTest lintRelease assembleRelease
```

Set the SDK location in `launcher/local.properties` or `ANDROID_HOME`. Some retained Python
tests have Windows compatibility issues; POSIX-only checks are skipped on Windows.

Based on the upstream project, under the [MIT license](LICENSE).
