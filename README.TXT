jGnash 2.x requires that version 7 or newer of the Java Platform be installed.
jGnash has been tested with the Oracle JVM as well as the open source OpenJDK Platform.

To Install:
1. Unzip all files into a directory of your choice leaving the directory structure unchanged.

To Run:
Simply type the following below at a command line
or double click on the jar or exe file in Windows.

java -jar jGnash2.jar

OpenJDK Tips:

If you are using the OpenJDK, enabling OpenGL acceleration can significantly improve
graphics performance.  See the integrated help for use of the "-opengl" option.

Linux Tips:

jGnash is not compatible with the GCJ Java installation preinstalled on older Linux distributions.
You will need to install the OpenJDK or Oracle Java Platform and correctly set the default for jGnash
to operate correctly.

OSX Installation:

For OSX users, a minimum of OSX 10.5 is required unless you want to experiment
with the SoyLatte Java distribution.

1.  Copy the jGnash folder to /Applications.
2.  Open Script Editor.
3.  Create the following script:

try
        do shell script "/System/Library/Frameworks/JavaVM.framework/Versions/
1.7.0/Home/bin/java -classpath /Applications/jGnash/lib -jar /
Applications/jGnash/jgnash2.jar"
end try

4.  Save it as an Application called jGnash.app in /Applications/jGnash

NOTES:

When upgrading from 1.x to 2.x, you will have to recreate your reminders.

See the integrated help for command line options.

Building jGnash:
jGnash requires Ant and Maven to build.  The Maven based jgnash-help project
must be run and installed into your local maven repository.  See the jgnash-help
README for details.

Install that Ant and Maven build systems and then try:

"mvn package" to create the distribution zip file

