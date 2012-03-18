/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus

 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/*
 * Large portions taken from http://code.google.com/p/maven-javahelp-plugin/source/browse/trunk/src/main/java/org/codehaus/mojo/javahelp/JavaHelpMojo.java
 */
package jgnash.tools;

import com.sun.java.help.search.Indexer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import org.codehaus.plexus.util.DirectoryScanner;

/**
 * This is a tool to index Javahelp files within Maven using only the javahelp2
 * dependency. Javahelp is not required to be installed at the OS level.
 *
 * This class is a replacement for the jhindexer executable class that is
 * included with the binary distribution of javahelp2 and an equivalent to
 * maven-javahelp-plugin which was not available in the codehaus repository at
 * the time.
 *
 * @author Craig Cavanaugh
 *
 */
public class HelpIndexer {

    private final String[] cmdArgs;
    private static final String DEFAULT_INCLUDE = "**/*.html";
    private static final String DEFAULT_EXCLUDE = "**/.svn";
    /**
     * List of files to include. Specified as fileset patterns.
     *
     * @parameter
     */
    private String[] includes;
    /**
     * List of files to exclude. Specified as fileset patterns.
     *
     * @parameter
     */
    private String[] excludes;
    /**
     * The directory containing the Java Help set.
     *
     * @parameter expression="${basedir}/src/main/resources"
     */
    @Option(name = "-source", usage = "Source of html files to index")
    private File sourcePath;
    /**
     * The location of the output JavaSearchIndex database.
     *
     * @parameter expression="${project.build.directory}" @required
     */
    @Option(name = "-db", usage = "Location to dump index database")
    private File dataBase;

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {                    
        HelpIndexer helpIndexer = new HelpIndexer(args);
        helpIndexer.execute();
    }

    public HelpIndexer(final String[] args) {
        cmdArgs = args;
    }

    public void execute() {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(cmdArgs);

            ArrayList<String> args = new ArrayList<>();

            if (dataBase != null) {
                args.add("-db");
                args.add(dataBase.getAbsolutePath());
            }

            if (sourcePath != null) {
                args.add("-sourcepath");
                args.add(sourcePath.getAbsolutePath() + File.separator);

                List<String> files = getFilesToIndex(sourcePath);
                args.addAll(files);
            }

            Indexer indexer = new Indexer();

            indexer.compile(args.toArray(new String[args.size()]));                                
        } catch (CmdLineException e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, e.getLocalizedMessage(), e);           
        } catch (Exception e) {
            Logger.getAnonymousLogger().log(Level.SEVERE, e.getLocalizedMessage(), e);  
        }
    }

    private List<String> getFilesToIndex(final File basedir) {
        ArrayList<String> files = new ArrayList<>();

        DirectoryScanner scanner = new DirectoryScanner();

        if (excludes == null || excludes.length == 0) {
            scanner.setExcludes(new String[]{DEFAULT_EXCLUDE});
        } else {
            scanner.setExcludes(excludes);
        }

        if (includes == null || includes.length == 0) {
            scanner.setIncludes(new String[]{DEFAULT_INCLUDE});
        } else {
            scanner.setIncludes(includes);
        }

        scanner.setBasedir(basedir);
        scanner.scan();

        files.addAll(Arrays.asList(scanner.getIncludedFiles()));

        return files;
    }
}
