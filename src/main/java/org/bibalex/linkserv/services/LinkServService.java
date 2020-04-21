package org.bibalex.linkserv.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bibalex.linkserv.handlers.JSONHandler;
import org.bibalex.linkserv.handlers.Neo4jHandler;
import org.bibalex.linkserv.handlers.PropertiesHandler;
import org.bibalex.linkserv.handlers.WorkspaceNameHandler;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;

@Service

public class LinkServService {

    private WorkspaceNameHandler workspaceNameHandler = new WorkspaceNameHandler();
    private Neo4jHandler neo4jHandler = new Neo4jHandler();
    private JSONHandler jsonHandler;

    private static final Logger LOGGER = LogManager.getLogger(LinkServService.class);

    public String getGraph(String workspaceName, Integer depth) {

        jsonHandler = new JSONHandler();

        Map<String, String> workspaceNameParameters = workspaceNameHandler.splitWorkspaceName(workspaceName);

        String url = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceURL"));
        String timestamp = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceTimestamp"));
        String jsonString = "";

        LOGGER.info("Get Graph of: " + url + " with Version: " + timestamp + " and Depth: " + depth);

        ArrayList<JSONObject> jsonArray = jsonHandler.getGraph(url, timestamp, depth);

        for(JSONObject json : jsonArray) {
            jsonString += (json.toString()) + "\n";
        }

        if (jsonString.isEmpty()) {
            LOGGER.info("No Match Found");
        } else {
            LOGGER.debug("Graph Returned: " + jsonString);
            LOGGER.info("Returned Match Successfully");
        }
        return jsonString;
    }

    public String updateGraph(String jsonData) {

        LOGGER.info("Update Graph");

        jsonHandler = new JSONHandler();
        jsonData = URLDecoder.decode(jsonData);
        if (jsonData.contains("&")) {
            jsonData = jsonData.split("&")[1];
        }
        String[] jsonLines = jsonData.split("\\r");
        for (String jsonLine : jsonLines) {
            jsonHandler.getProperties(jsonLine);

        }
        boolean done = neo4jHandler.addNodesAndRelationships(jsonHandler.getData());
        if (done) {
            jsonData = jsonData.replace("=", "");
            jsonData = jsonData.replace("\\r", "");

            LOGGER.info("Graph Updated Successfully");
            LOGGER.debug("JSON Data: " + jsonData);
            return jsonData + "\n";
        } else
            LOGGER.info("Could not Update Graph");
            LOGGER.debug("JSON Data: " + jsonData);
            return "";
    }
}
