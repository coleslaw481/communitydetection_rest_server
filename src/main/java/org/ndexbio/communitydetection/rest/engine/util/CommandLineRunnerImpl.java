package org.ndexbio.communitydetection.rest.engine.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class CommandLineRunnerImpl implements CommandLineRunner {
      private static final Logger _log
            = LoggerFactory.getLogger(CommandLineRunnerImpl.class.getName());
    
    private String _workingDirectory;
    private Map<String, String> _environVars;
    private String _lastCommand;
    
    /**
     * Sets the working directory for the process
     * @param workingDir path to working directory
     */
    @Override
    public void setWorkingDirectory(final String workingDir) {
        _workingDirectory = workingDir;
    }

    /**
     * Sets environment variables for process
     * @param envVars map where key is environment name and value is the value
     */
    @Override
    public void setEnvironmentVariables(Map<String, String> envVars) {
        _environVars = envVars;
    }

    /**
     * Gets the last command as a space delimited string
     * @return Last command as string or null if no commands have been run
     */
    @Override
    public String getLastCommand() {
        return _lastCommand;
    }
    
    /**
     * Runs command line process
     * @param timeOut timeout value
     * @param unit unit for timeout value
     * @param stdOutFile File to write any standard output
     * @param stdErrFile File to write any standard
     * @param command command with arguments to run
     * @return exit code of process (if timeout exceeded, 500 is returned)
     * @throws Exception if there is an error
     */
    @Override
    public int runCommandLineProcess(long timeOut, TimeUnit unit,
            File stdOutFile, File stdErrFile, String... command) throws Exception {        
        ArrayList<String> mCmd = new ArrayList<String>();
        _lastCommand = null;
        StringBuilder lastCmdSb = new StringBuilder();
        for (String c : command) {
            if (c.equals("")){
                continue;
            }
            mCmd.add(c);
            if (lastCmdSb.length() > 0){
                lastCmdSb.append(" ");
            }
            lastCmdSb.append(c);
        }
        _lastCommand = lastCmdSb.toString();
        
        _log.debug("Running command: " + _lastCommand);
        String[] yo = new String[mCmd.size()];
        ProcessBuilder pb = new ProcessBuilder(mCmd.toArray(yo));

        //lets caller set working directory
        if (_workingDirectory != null) {
            pb.directory(new File(_workingDirectory));
        }

        //lets caller set 1 or more environment variables
        if (_environVars != null && _environVars.isEmpty() == false) {
            Map<String, String> env = pb.environment();
            for (String key : _environVars.keySet()) {
                env.remove(key);
                env.put(key, _environVars.get(key));
            }
        }
        pb.redirectError(stdErrFile);
        pb.redirectOutput(stdOutFile);

        Process proc = pb.start();
        
        if (proc.waitFor(timeOut, unit) == false){
            proc.destroyForcibly();
            return 500;
        } 
        return proc.exitValue();        
    }
}
