package org.bibalex.linkserv.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class WorkspaceNameHandler {

    private static final Logger LOGGER = LogManager.getLogger(WorkspaceNameHandler.class);

    public Map<String, String> splitWorkspaceName(String workspaceName){

        LOGGER.info("Splitting: \"" + workspaceName + "\"");
        Map<String, String> workspaceParameters =  new HashMap<String, String>();
        String [] parameters = workspaceName.split(",",2);

        workspaceParameters.put(PropertiesHandler.getProperty("workspaceTimestamp"), parameters[0]);
        workspaceParameters.put(PropertiesHandler.getProperty("workspaceURL"), parameters[1]);

        LOGGER.info("URL: " + workspaceParameters.get("url"));
        LOGGER.info("Timestamp: " + workspaceParameters.get("timestamp"));

        return workspaceParameters;
    }
}
