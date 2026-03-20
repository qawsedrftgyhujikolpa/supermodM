Gradle Wrapper & Build Instructions

1) If you have Gradle installed locally:
   - From the project root run: gradle wrapper
   - This generates gradlew, gradlew.bat and gradle/wrapper/gradle-wrapper.jar
   - Then build with: ./gradlew build  (Windows: gradlew.bat build)

2) If you cannot install Gradle on this machine:
   - On another machine with Gradle: run gradle wrapper in the project root
   - Copy the generated files into this project:
     * gradlew
     * gradlew.bat
     * gradle/wrapper/gradle-wrapper.jar
     * gradle/wrapper/gradle-wrapper.properties

3) Requirements:
   - JDK 17 (JAVA_HOME set)
   - Recommended Gradle version: 8.4.1 (adjust if fabric-loom requires another)

4) Notes:
   - This repository includes placeholder gradlew scripts and gradle/wrapper/gradle-wrapper.properties but not the gradle-wrapper.jar (it cannot be generated here).
   - After building, mod JAR will be in build/libs/

5) If you'd like, I can try a remote build on a machine that has Gradle installed—ask and provide access or run locally.