Generating the Gradle wrapper (gradle-wrapper.jar) and building locally

If you want to build this mod locally you need the Gradle wrapper JAR in gradle/wrapper/gradle-wrapper.jar.
This environment doesn't have the wrapper JAR, so generate it on a machine with Gradle installed and copy the generated files into this project.

1) On a machine with Gradle installed
 - Open a terminal in the project root (folder containing build.gradle).
 - Run: gradle wrapper
   This generates gradle/wrapper/gradle-wrapper.jar and may update gradlew/gradlew.bat.
 - Commit the generated gradle/wrapper/gradle-wrapper.jar to the repo.

2) Build the mod using the wrapper (Windows)
 - In the project root run: gradlew.bat build
   (or on *nix: ./gradlew build)
 - The built mod JAR will be in build/libs/.

3) Common troubleshooting
 - Ensure JDK 17 is installed and JAVA_HOME points to it.
 - If mappings/minecraft dependency fails, run the build once to let Gradle download dependencies.
 - If you cannot install Gradle, use a CI runner (GitHub Actions) to run 'gradle wrapper' and commit the generated wrapper files.

Want me to generate a GitHub Actions workflow that runs the wrapper and builds the mod automatically? Reply and I can add it.
