![jGnash Logo](https://sourceforge.net/apps/mediawiki/jgnash/nfs/project/j/jg/jgnash/f/f3/Jgnash-logo.png)

# jGnash README

[jGnash](https://sourceforge.net/apps/mediawiki/jgnash/index.php?title=Main_Page) is a free (no strings attached!) personal finance manager with many of the same features as commercially-available software. It was created in order to make tracking your personal finances easy, but also provides the functionality required by advanced users. jGnash is cross-platform: jGnash 2 will run on any operating system that has a working Java 7 Runtime Environment (e.g., Linux, Mac OS X, and Microsoft Windows)

jGnash 2.x requires that the Java Platform version 7 or newer be installed.
jGnash has been tested with the Oracle JVM as well as the open source OpenJDK Platform.


## Getting Java

Most users of jGnash will want the latest version of [Oracle Java Runtime Environment](http://www.java.com/en/download/).

Developers will want the Java Development Kit (see build instructions below.)

## Learn about jGnash

To learn about the features of jGnash, visit the [jGnash Website](https://sourceforge.net/apps/mediawiki/jgnash/index.php?title=Main_Page).

## Download jGnash

You can downlaod jGnash from the [jGnash Download Page](https://sourceforge.net/projects/jgnash/files/Active%20Stable%202.x/).

## To Install jGnash:

1. Unzip all files into a directory of your choice leaving the directory structure unchanged.

## To Run:

Simply type the following below at a command line
or double click on the jar or exe file in Windows.

    java -jar jGnash2.jar

### OpenJDK Tips:

If you are using the OpenJDK, enabling OpenGL acceleration can significantly improve
graphics performance.  See the integrated help for use of the ```-opengl``` option.

### Linux Tips:

jGnash is not compatible with the GCJ Java installation preinstalled on older Linux distributions.
You will need to install the OpenJDK or Oracle Java Platform and correctly set the default for jGnash
to operate correctly.

### Mac OS X Installation:

For Mac OS X users, a minimum of Mac OS X 10.7 is required unless you want to experiment with the SoyLatte Java distribution.

1. Copy the jGnash folder to ```/Applications```.
2. Open AppleScript Editor.
3. Create the following script:

        try
            do shell script "/System/Library/Frameworks/JavaVM.framework/Versions/ 1.7.0/Home/bin/java -classpath /Applications/jGnash/lib -jar /Applications/jGnash/jgnash2.jar"
        end try

4. Save it as an Application called ```jGnash.app``` in ```/Applications/jGnash```

### NOTES:

When upgrading from 1.x to 2.x, you will have to recreate your reminders.

See the integrated help for command line options.

## Building jGnash:

To build JGnash you'll need the following software installed on your system:

1. [JDK 7](http://www.oracle.com/technetwork/java/javase/downloads/index.html) or later.
1. [Apache Ant](http://ant.apache.org) 1.7.0 or later
1. [Apache Maven](http://maven.apache.org) 3.0 or later

Before running the main build, the Maven-based ```jgnash-help``` project
must be built and installed into your local Maven repository:

    cd jgnash-help
    mvn install

To create the distribution zip file, return to the main directory (```cd ..```) and then run:

    mvn package

The distribution zip file will be produced at ```jgnash-swing/target/jgnash-```_version_```-bin.zip```.

