[The jolt-jni-docs project][project] provides
documentation and sample applications
for [the Jolt-JNI physics library][joltjni].

It contains 2 subprojects:

1. docs: [Antora] documentation for Jolt JNI, including a tutorial
2. java-apps: sample applications referred to in the tutorial, written in [Java]

The sample applications utilize [the Sport graphics engine for Jolt][sportjolt],
which is a separate open-source project at [GitHub].

Complete source code is provided under
[a 3-clause BSD license][license].


## How to build and run jolt-jni-docs from source

### Sample applications

1. Install a [Java Development Kit (JDK)][adoptium],
   if you don't already have one.
2. Point the `JAVA_HOME` environment variable to your JDK installation:
   (In other words, set it to the path of a directory/folder
   containing a "bin" that contains a Java executable.
   That path might look something like
   "C:\Program Files\Eclipse Adoptium\jdk-17.0.3.7-hotspot"
   or "/usr/lib/jvm/java-17-openjdk-amd64/" or
   "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home" .)
  + using Bash or Zsh: `export JAVA_HOME="` *path to installation* `"`
  + using [Fish]: `set -g JAVA_HOME "` *path to installation* `"`
  + using Windows Command Prompt: `set JAVA_HOME="` *path to installation* `"`
  + using PowerShell: `$env:JAVA_HOME = '` *path to installation* `'`
3. Download and extract the jolt-jni-docs source code from GitHub:
  + using [Git]:
    + `git clone https://github.com/stephengold/jolt-jni-docs.git`
    + `cd jolt-jni-docs`
4. Run the [Gradle] wrapper:
  + using Bash or Fish or PowerShell or Zsh: `./gradlew build`
  + using Windows Command Prompt: `.\gradlew build`

### Antora documentation

1. Edit "docs/playbook.yml" and replace "/home/sgold/NetBeansProjects/jolt-jni-docs"
  with an absolute path to your checkout directory (2 places).
2. [Install Node.js](https://docs.antora.org/antora/latest/install-and-run-quickstart/#install-nodejs)
3. Run the [Antora] site generator:
  + `npx antora docs/playbook.yml`

### Cleanup

You can restore the project to a pristine state:
+ using Bash or Fish or PowerShell or Zsh: `./gradlew clean`
+ using Windows Command Prompt: `.\gradlew clean`


[adoptium]: https://adoptium.net/releases.html "Adoptium"
[antora]: https://antora.org/ "Antora site generator"
[fish]: https://fishshell.com/ "Fish command-line shell"
[git]: https://git-scm.com "Git version-control system"
[github]: https://en.wikipedia.org/wiki/GitHub "GitHub"
[gradle]: https://gradle.org "Gradle build tool"
[java]: https://en.wikipedia.org/wiki/Java_(programming_language) "Java programming language"
[joltjni]: https://github.com/stephengold/jolt-jni "Jolt-JNI project"
[license]: https://github.com/stephengold/jolt-jni-docs/blob/master/LICENSE "jolt-jni-docs license"
[project]: https://github.com/stephengold/jolt-jni-docs "jolt-jni-docs project"
[sportjolt]: https://github.com/stephengold/sport-jolt "Sport graphics engine for Jolt"
