package org.ndexbio.communitydetection.rest.engine.util;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author churas
 */
public interface CommandLineRunner {
    
    /**
     * Sets working directory
     * @param workingDir path to directory
     */
    public void setWorkingDirectory(final String workingDir);

    /**
     * Sets environment variables for task
     * @param envVars Map where key is environment variable name and value is the value
     *                for that environment variable
     */
    public void setEnvironmentVariables(Map<String, String> envVars);

    /**
     * Last command run
     * @return the last command run
     */
    public String getLastCommand();
    
    /**
     * Runs command line program specified by first argument.
     * @param timeOut time in unit specified by {@code unit} parameter
     * @param unit unit of time for {@code timeOut}
     * @param stdOutFile destination for any output to standard out
     * @param stdErrFile destination for any output to standard error
     * @param command - First argument should be full path to command followed by arguments
     * @return containing exit code of program or 500 if the process exceeded timeout
     * @throws java.lang.Exception if there was an error invoking the process
     */
    public int runCommandLineProcess(long timeOut, TimeUnit unit, File stdOutFile, File stdErrFile, String... command) throws Exception;
    
}
