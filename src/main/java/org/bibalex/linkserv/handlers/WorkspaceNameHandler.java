package org.bibalex.linkserv.handlers;

import java.util.HashMap;
import java.util.Map;

public class WorkspaceNameHandler {

    public Map<String, String> splitWorkspaceName(String workspaceName){

        Map<String, String> workspaceParameters =  new HashMap<String, String>();
        String [] parameters = workspaceName.split(",");

        workspaceParameters.put(PropertiesHandler.getProperty("workspaceTimestamp"), parameters[0]);
        workspaceParameters.put(PropertiesHandler.getProperty("workspaceURL"), parameters[1]);

        return workspaceParameters;
    }
}
