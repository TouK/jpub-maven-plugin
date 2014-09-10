/*
 * Copyright (c) 2008 TouK.pl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.touk.mojo

import org.apache.maven.plugin.MojoExecutionException
import org.codehaus.gmaven.mojo.GroovyMojo

/**
 * Phase which generates jpublisher classes.
 *
 * @goal unpack
 * @phase generate-sources
 * @author kn@touk.pl
 */
class JPubMojo extends GroovyMojo {


    /**
     * Location of the source files.
     *
     * @parameter expression="$ {basedir} /src/main/java"
     * @required
     */
    private File sourceDirectory;

    /**
     * Location of the generated files.
     *
     * @parameter expression="$ {project.build.directory} /classes"
     * @required
     */
    private File outputDirectory;

    /**
     * should java beans implement java.io.Serializable?
     *
     * @parameter
     */
    private Boolean serializable;

    /**
     * skip generation?
     *
     * @parameter
     */
    private Boolean skip;
    /**
     * show jpub output, works only under linux #!%$^&*
     *
     * @parameter
     */
    private Boolean debug;
    /**
     * generated package name
     *
     * @parameter
     * @required
     */
    private String genPackage;
    /**
     * compile source code true/false
     *
     * @parameter
     */
    private String compile;

    /**
     *  Source code encoding
     *
     * @parameter
     */
    private String encoding;

    /**
     * url to database
     *
     * @parameter
     * @required
     */
    private String url;

    /**
     * Connection scope - go for method for thread safety
     * class/method
     * @parameter
     */
    private String connscope;

    /**
     * dbuser/password
     *
     * @parameter
     * @required
     */
    private String user;

    /**
     * javac file location
     *
     * @parameter
     *
     */
    private String compilerExecutable;
    /**
     * -toString - generate toString methods for java beans
     *
     * @parameter
     */
    private Boolean toString;

    /**
     * other jpub arguments
     *
     * @parameter
     */
    private String other;
    /**
     * Java -Xms paramter value;
     *
     * @parameter
     */
    private String xms;

    /**
     * Java -Xmx paramter value;
     *
     * @parameter
     */
    private String xmx;

    /**
     * Java -Xss paramter value;
     *
     * @parameter
     */
    private String xss;

    /**
     * If this parameter is set to true closeConnection() method is called in every finally block. So if you are using the pool logical connection will be closed and returned to the pool.
     *
     * Defeault to true;
     * @parameter
     */
    private Boolean closeConnection;

    /**
     * If this parameter is set to true closeInvalidConnection() method is called in every catch SQLException block.
     * It marks used connections as invalid so that underlying physical connection will also be closed and removed from the pool.
     * So if you expect lots of SQLException you may kill the database.
     * It works only if closeConnection is set to true too!!!
     *
     * Defeault to true;
     * @parameter
     */
    private Boolean closeInvalidConnection;

    public void execute() throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("JPubMojo#execute()");
        }

        if (skip != null && skip) {
            getLog().warn("\n\nskipping jpub code generation!!!\n\n");
            return;
        }
        final String JPUB_CLASS_NAME = "oracle.jpub.java.Main";

        // builds java command
        StringBuffer command = new StringBuffer();
        String FILE_SEPARATOR = System.getProperty("file.separator");
        command.append(System.getProperty("java.home") + FILE_SEPARATOR + "bin"
                + FILE_SEPARATOR + "java");

        // builds java arguments
        StringBuffer arguments = new StringBuffer();
        URLClassLoader ucl = (URLClassLoader) getClassLoader();

        // sets jvm arguments
        if (xms != null)
            arguments.append(" -Xms" + xms + " ");
        if (xmx != null)
            arguments.append(" -Xmx" + xmx + " ");
        if (xss != null)
            arguments.append(" -Xss" + xss + " ");

        // sets classpath
        arguments.append(" -classpath ");

        String PATH_SEPARATOR = System.getProperty("path.separator");
        boolean isWindows = false;
        if (!FILE_SEPARATOR.equals("/")) {
            isWindows = true;
        }

        for (URL u: ucl.getURLs()) {

            if (isWindows) {
                arguments.append(convertPathToMSWindows(u.getPath()));
            } else {
                arguments.append(u.getPath());
            }
            arguments.append(PATH_SEPARATOR);
        }

        arguments.append(" " + JPUB_CLASS_NAME + " ");

        // sets jpublisher options

        arguments.append("-user=" + user + " ");

        arguments.append("-url=" + url + " ");

        arguments.append("-package=" + genPackage + " ");

        arguments.append("-dir=" + sourceDirectory.getAbsolutePath() + " ");

        arguments.append("-compile=" + compile + " -d " + outputDirectory.getAbsolutePath() + " ");

        if (other != null)
            arguments.append(" " + other + " ");

        if (connscope != null)
            arguments.append("-connscope=" + connscope + " ");

        if (compilerExecutable == null || compilerExecutable.trim().length() == 0) {
            compilerExecutable = System.getProperty("java.home") + FILE_SEPARATOR + "bin" + FILE_SEPARATOR + "javac";
            if (!(new File(compilerExecutable).exists())) {
                compilerExecutable = System.getProperty("java.home") + FILE_SEPARATOR + ".." + FILE_SEPARATOR + "bin" + FILE_SEPARATOR + "javac";
                if (!(new File(compilerExecutable).exists())) {
                    getLog().warn("please provide compilerExecutable parameter because I can't find javac file");
                    compilerExecutable = "javac";
                }
            }
        }
        arguments.append("-compiler-executable=" + compilerExecutable + " ");

        if (encoding != null)
            arguments.append("-encoding=" + encoding + " ");

        if (toString != null && toString)
            arguments.append(" -toString=true ");
        else
            arguments.append(" -toString=false ");


        if (serializable != null && serializable)
            arguments.append(" -serializable=true ");
        else
            arguments.append(" -serializable=false ");

        getLog().info("create directory : " + outputDirectory.mkdirs() + " " + outputDirectory.exists());

        getLog().info("jpub classpath :" + command.toString());
        getLog().info("jpub arguments :" + arguments.toString());


        try {
            Process p = Runtime.getRuntime().exec(
                    command.toString() + " " + arguments.toString());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String line = null;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null)
                sb.append(line + System.getProperty("line.separator"));

            int compilationStatus = p.waitFor();

            if (debug != null && debug)
                getLog().info("jpub: JPublisher status = " + compilationStatus + sb.toString());
            getLog().info("jpub: JPublisher status = " + compilationStatus);

            if (compilationStatus != 0) {
                getLog().error("jpub: JPublisher error log: " + sb.toString());

                throw new MojoExecutionException(
                        "Compiler thread stopped with compilation status = "
                                + compilationStatus);
            } else {

                if (closeConnection == null || closeConnection) {
                    addCloseConnectionToGeneratedFiles();
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Compiler thread stopped.", e);
        }

    }

    //for i in `grep -ilr ' { __sJT_ec.oracleClose(); }' *`; do cat $i | sed "s/\(.*{ \)\(__sJT_ec.oracleClose(); \)\(}.*\)/\1\2 closeConnection();\3/g"; done
    private void addCloseConnectionToGeneratedFiles() {

        def includesDir = "**/" + genPackage.replaceAll("\\.", "\\/") + "/*.java";
        def ant = new AntBuilder();

        
        if (closeInvalidConnection == null || closeInvalidConnection) {
            log.info("adding closeInvalidConnection() method to generated classes in " + sourceDirectory.getAbsolutePath() + " " + includesDir)
            log.info("make sure you re-compile generated classes afterwards");
            ant.replaceregexp(match: "      closeConnection\\(\\);", replace: "      closeInvalidConnection();", byline: false, flags: "g") {
                fileset(dir: sourceDirectory.getAbsolutePath(), includes: includesDir)
            }
            def cicBody = "  public void closeInvalidConnection\\(\\)\\{\\\n" +
                    "    if \\(__dataSource!=null\\) \\{\\\n" +
                    "      try \\{ if \\(__onn!=null\\) \\{ \\(\\(oracle.jdbc.OracleConnection\\)__onn\\).close\\(oracle.jdbc.OracleConnection.INVALID_CONNECTION\\); \\} \\} catch \\(java.sql.SQLException e\\) \\{\\}\\\n" +
                    "      try \\{ if \\(__tx!=null\\) \\{__tx.close\\(\\); \\} \\} catch \\(java.sql.SQLException e\\) \\{\\}\\\n" +
                    "      __onn=null;\\\n" +
                    "      __tx=null;\\\n" +
                    "    \\}\\\n" +
                    "  \\}\\\n\\\n" +
                    "  public void closeConnection\\(\\)\\{";
            ant.replaceregexp(match: "  public void closeConnection\\(\\)\\{", replace: cicBody, byline: false, flags: "g") {
                fileset(dir: sourceDirectory.getAbsolutePath(), includes: includesDir)
            }

            def catchSEX = " \\} catch\\(java.sql.SQLException _err\\) \\{\\\n" + 
                    "    try \\{\\\n" + 
                    "      getConnectionContext\\(\\).getExecutionContext\\(\\).close\\(\\);\\\n" + 
                    "      closeInvalidConnection\\(\\);\\\n" + 
                    "      throw _err;\\\n";
            ant.replaceregexp(match: "(.*finally \\{)( __sJT_ec.oracleClose\\(\\); )(\\}.*)", replace: catchSEX + "\\1\\2 closeConnection();\\3", byline: false, flags: "g") {
                fileset(dir: sourceDirectory.getAbsolutePath(), includes: includesDir)
            }
        } // end of closeInvalidConnection
        else { 
            log.info("adding closeConnection() method to generated classes in " + sourceDirectory.getAbsolutePath() + " " + includesDir)
            log.info("make sure you re-compile generated classes afterwards");
            ant.replaceregexp(match: "(.*finally \\{)( __sJT_ec.oracleClose\\(\\); )(\\}.*)", replace: "\\1\\2 closeConnection();\\3", byline: false, flags: "g") {
                fileset(dir: sourceDirectory.getAbsolutePath(), includes: includesDir)
            }
        }
    }

    private ClassLoader getClassLoader() throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("JPubMojo#getClassLoader()");
        }

        URLClassLoader myClassLoader = (URLClassLoader) getClass().getClassLoader();

        // TODO : Due to PLX-220, we must convert the classpath URLs to escaped
        // URI form.
        // cf. http://jira.codehaus.org/browse/PLX-220
        URL[] originalUrls = myClassLoader.getURLs();
        URL[] urls = new URL[originalUrls.length + 1];
        for (int index = 0; index < originalUrls.length; ++index) {
            try {
                String url = originalUrls[index].toExternalForm();
                urls[index] = new File(url.substring("file:".length())).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(
                        "Failed to convert original classpath to URL.", e);
            }
        }

        // TODO : can we have the gwt source directory already in the classpath?
        try {
            urls[originalUrls.length] = sourceDirectory.toURL();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(
                    "Failed to convert source root to URL.", e);
        }
        /*
         * int count = 0; for(java.util.Iterator itr =
         * sourceDirectories.iterator(); itr.hasNext(); count++ ){ try { File
         * sourceDirectory = new File(itr.next().toString());
         * urls[originalUrls.length + count] = sourceDirectory.toURL(); } catch
         * (MalformedURLException e) { throw new
         * MojoExecutionException("MalformedURLException", e); } }
         */
        if (getLog().isDebugEnabled()) {
            for (int i = 0; i < urls.length; i++) {
                getLog().debug("  URL:" + urls[i]);
            }
        }

        return new URLClassLoader(urls, (ClassLoader)myClassLoader.getParent());


    }

    private String convertPathToMSWindows(String path) {
        if (path != null && path.length() == 0)
            return null;
        String newPath = path.substring(1);
        path = newPath.replace('/', '\\');
        return path;
    }
}
