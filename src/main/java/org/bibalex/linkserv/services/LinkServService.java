package org.bibalex.linkserv.services;

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


    public String getGraph(String workspaceName, Integer depth) {

        jsonHandler = new JSONHandler();

        Map<String, String> workspaceNameParameters = workspaceNameHandler.splitWorkspaceName(workspaceName);

        String url = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceURL"));
        String timestamp = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceTimestamp"));
        String jsonString = "";

        ArrayList<JSONObject> jsonArray = jsonHandler.getGraph(url, timestamp, depth);

        for(JSONObject json : jsonArray) {
            jsonString += (json.toString()) + "\n";
        }
        return jsonString;
    }

    public String updateGraph(String jsonData) {

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
            return jsonData + "\n";
        } else
            return "";
    }
}
