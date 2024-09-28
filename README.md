<span class="image">![jGnash Logo](https://jgnash.github.io/img/jgnash-logo.png)</span>

## jGnash README

[jGnash](https://sourceforge.net/projects/jgnash/) is a free (no strings attached!) personal finance manager with many of the same features as commercially-available software. It was created in order to make tracking personal finances easy, but also provides the functionality needed by advanced users. jGnash is cross-platform and will run on any operating system that has a current Java Runtime Environment (e.g., Linux, Mac OS X, and Microsoft Windows).

-   jGnash requires **Java 11** or newer and is compatible with the open source OpenJDK Platform, and the Oracle JVM as well.

See the [Requirements](#Requirements) section below for more details.

### Contents:

-   [About jGnash](#About)

    -   [jGnash Features](#Features)

-   [Donations](#Donations)

-   [Support](#Support)

-   [Requirements](#Requirements)

    -   [Java](#Reqs-Java)

    -   [Supported Operating System versions](#Reqs-OS)

-   [Download jGnash](#Download)

-   [Installation](#Install)

-   [Running jGnash](#Running)

-   [Building and Development](#Development)

## About jGnash

### jGnash Features

-   Operates on any operating system with Java 11 or newer installed

-   Double Entry Accounting with reconciliation tools

-   OFX, QFX, mt940, and QIF import capabilities

-   Investment Accounts and automatic import of Stocks, Bond, and Funds price history

-   Nestable accounts with automatic rollup of totals and intelligent handling of mixed currencies

-   Reminders with automatic transaction entry

-   Intelligent handling of multiple currencies and exchange rates with automatic online exchange rate updates

-   Printable reports with PDF and spreadsheet export capability

-   XML, Binary, and multiple relational database file formats

-   Supports concurrent multiple users over a network

To learn more about the features of jGnash, visit the [jGnash Website](https://sourceforge.net/projects/jgnash/).

The jGnash download includes a user manual to help get you started with the basics if you are new to tracking finances. It also covers some of the more subtle features, command line options, and shortcuts that are not immediately obvious.

The latest version of jGnash uses **OpenJFX** for the user interface. This replaces the old version that used Java Swing for the user interface. Experienced jGnash users will notice interface improvements. For example, try using the vertical and horizontal scroll wheels in a date picker, and the collapsible transaction forms.

## Donations

Donations are always welcome and appreciated. This helps to defer the cost of computer hardware and internet access.

[<span class="image">![PayPal](https://img.shields.io/badge/Donate-PayPal-green.svg)</span>](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TYN4QECUL5C44)

## Support

The **[jGnash Help Group](https://groups.google.com/forum/#!forum/jgnash-user)** is always a good source if you need help and **is the prefered method of contact.** Your first post to the group will be moderated to filter spam.

Please use the search tool to check for similar questions.

The preferred method of reporting bugs is to use the [Github Issue tracker](https://github.com/ccavanaugh/jgnash/issues).

## Requirements

### 1. Java

Java 11 or newer is required to run jGnash. Unless you have a specific need for a newer version, Java 11 is currently recommended.

Use of a prebuilt installer is recommended.

-   [Azul OpenJDK 11](https://www.azul.com/downloads/zulu/) is a branded release that will be easiest to install for most users and is free to use.

-   [AdoptOpenJDK](https://adoptopenjdk.net/index.html?variant=openjdk11&jvmVariant=hotspot) will require manual installation but allows more flexibility and is free to use.

-   [https://jdk.java.net/11 (OpenJDK)/](https://jdk.java.net/11/) will require manual installation and is free to use.

-   [Oracle Java SE 11](https://www.oracle.com/technetwork/java/javase/downloads/index.html) will require manual installation and licensing is required.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr>
<td class="icon"><div class="title">
Note
</div></td>
<td class="content">When performing a manual installation of Java, The <strong>JAVA_HOME</strong> Environment Variable must be set. Also, the Java bin directory must be added to the execution path.</td>
</tr>
</tbody>
</table>

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr>
<td class="icon"><div class="title">
Note
</div></td>
<td class="content">If you have multiple versions of Java installed on your system, The <strong>JAVA_HOME</strong> Environment Variable must be set to Java 11 or newer and the related Java bin directory must be the only version in the execution path. Mixing JVM and JDK versions will confuse the bootloader.</td>
</tr>
</tbody>
</table>

***Use of an OpenJDK package is recommended over use of Oracle JDK due to licensing requirements***

### 2. OpenJFX

jGnash uses OpenJFX for the user interface, but will automatically download and place the needed components within the lib directly of the jGnash installation. Portions of the OpenJFX components are OS specific and cannot be shared between different operating systems.

### 3. Supported Operating Systems: Windows, Linux, or Mac OS X.

#### Microsoft Windows

-   any Windows release that can run the required version of Java

#### Linux

-   any Linux distribution that can run the required version of Java

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr>
<td class="icon"><div class="title">
Note
</div></td>
<td class="content">jGnash is <em>not compatible</em> with GCJ pre-installed on older Linux distributions. You will need to install <strong>OpenJDK 11</strong> for jGnash to operate correctly.</td>
</tr>
</tbody>
</table>

#### Mac OS X

-   Mac OS X 10.8.3 or later

-   can run the required version of Java

*Be sure to read [the section about installing on Mac OS X](#Install-MacOSX) to create the startup script.*

## Download jGnash

You can download jGnash from the [jGnash Download Page](https://sourceforge.net/projects/jgnash/files/Active%20Stable%202.x/). <span class="image"><a href="https://sourceforge.net/projects/jgnash/files/latest/download" class="image"><img src="https://img.shields.io/sourceforge/dt/jgnash.svg" alt="Download button" /></a></span>

## To Install jGnash

1.  Install the latest version of **Java 11** if you don’t already have it installed. *jGnash has been tested and is know to work on Java 12 through 14.*

    -   Developers will want the complete Java Development Kit (see build instructions below.)

2.  Unzip all files into a directory of your choice leaving the directory structure unchanged.

### Windows Installation:

Some Windows users with restricted rights may experience write access issues **(Access is denied exception)** with jGnash downloading the JavaFX dependencies.

Unzipping and placing jGnash into `%AppData%\jGnash` will ensure the users has proper write access.

### Mac OS X Installation:

1.  Copy the jGnash folder to `/Applications` and remove the version extension so that the final path looks like `/Applications/jGnash`.

2.  Create an AppleScript that will run the application:

    1.  Open the AppleScript Editor.

    2.  Create the following script:

            try
                do shell script "/Applications/jGnash/jGnash"
            end try

    3.  Save it as an Application called `jGnash.app` in `/Applications/jGnash`

3.  Instead of step 2, you can set the `/Applications/jGnash/jGnash` file to *Open with…​* `Terminal.app` (the Terminal application).

## To Run:

Executable files are provided for Windows and UN\*X users at the root of the installation directory. (These are `.exe` and `bash shell` files, respectively). Mac OS X users will have created application launch files per the [Mac installation instructions.](#Install-MacOSX)

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr>
<td class="icon"><div class="title">
Note
</div></td>
<td class="content">jGnash will need to be restarted after the first launch of a new version. Operating System specific files are download and a restart is required for correct operation.</td>
</tr>
</tbody>
</table>

-   Windows: Simply double-click on the jGnash.exe file.

-   UN\*X / MacOS: Start jGnash with the provided **jGnash** Bash script. If jGnash fails to launch, check your file permissions and make sure they are set to be executable or use an unzip tool that preserves file permissions.

An example for UN\*X users is shown below assuming you have changed to the installation directory:

``` CodeRay
./jGnash
```

**Mac OS X:** Run the application file you created per the [Mac installation instructions.](#Install-MacOSX)

## Building and Development

Travis-CI Build Status <span class="image"><a href="https://travis-ci.org/ccavanaugh/jgnash" class="image"><img src="https://travis-ci.org/ccavanaugh/jgnash.svg?branch=master" alt="Build Status" /></a></span>

### Development List

The [Google Groups jGnash Developer list](https://groups.google.com/forum/#!forum/jgnash-devel) is the best place to start if you have questions or ideas. Initial posts will are moderated to prevent spam.

### Development Tools

The IDE used for the development of jGnash is IntelliJ IDEA, but any IDE that supports a Gradle build environment should work.

<span class="image"><a href="https://www.jetbrains.com/idea/" class="image"><img src="https://github.com/jGnash/jgnash.github.io/blob/master/img/logo_IntelliJIDEA.png" height="90" alt="IntelliJIDEA Logo" /></a></span>

### Building jGnash:

**Gradle** is used as the primary build system for jGnash. The Gradle Wrapper is included (`gradlew` shell and .bat files) so that you do not need to install Gradle. The Wrapper will automatically download the necessary dependencies.

<table>
<colgroup>
<col style="width: 50%" />
<col style="width: 50%" />
</colgroup>
<tbody>
<tr>
<td class="icon"><div class="title">
Note
</div></td>
<td class="content">Depending on your OS (almost always Windows and OSX) the JCE Unlimited Strength Jurisdiction Policy Files for Java are needed for the unit tests to complete correctly. If you do not want to install these files or are restricted by your locale, modify the test build or disable tests. jGnash uses encryption for client / server communication and unit tests are performed to prevent regressions.</td>
</tr>
</tbody>
</table>

To build jGnash you’ll need the following software installed and correctly configured on your system:

OpenJDK 11 or later.

*If you are building with a recent 64bit Linux system, you may need to enable Multilib/32 Bit support capabilities. Otherwise, the Gradle build may fail when building the windows executables.*

To create the distribution zip file, start at the main directory and run the gradle task to clean and create the distribution:

**Building on Windows:**

``` CodeRay
gradlew clean distZip
```

**Building on UN\*X or Mac OS X:**

``` CodeRay
./gradlew clean distZip
```

This will run the Gradle tasks necessary to execute core tests and create the distribution file. The distributable zip file will be produced at the root of the build directory called jGnash-*version*-bin.zip.

Last updated 2020-03-24 06:00:42 -0400