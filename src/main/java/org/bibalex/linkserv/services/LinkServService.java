package org.bibalex.linkserv.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bibalex.linkserv.handlers.JSONHandler;
import org.bibalex.linkserv.handlers.Neo4jHandler;
import org.bibalex.linkserv.handlers.PropertiesHandler;
import org.bibalex.linkserv.handlers.WorkspaceNameHandler;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

@Service
public class LinkServService {

    private WorkspaceNameHandler workspaceNameHandler = new WorkspaceNameHandler();
    private Neo4jHandler neo4jHandler = new Neo4jHandler();
    private JSONHandler jsonHandler;

    private static final Logger LOGGER = LogManager.getLogger(LinkServService.class);

    public String getGraph(String workspaceName, Integer depth) {

        String timeRangeDelimiter = PropertiesHandler.getProperty("timeRangeDelimiter");
        jsonHandler = new JSONHandler(false);

        ArrayList<String> graphArray;

        Map<String, String> workspaceNameParameters = workspaceNameHandler.splitWorkspaceName(workspaceName);

        if (workspaceNameParameters == null)
            return PropertiesHandler.getProperty("badRequestResponseStatus");

        String url = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceURL"));
        String timestamp = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceTimestamp"));

        if (timestamp.contains(timeRangeDelimiter)) {
            String[] timestamps = timestamp.split(timeRangeDelimiter, 2);
            String startTimestamp = timestamps[0];
            String endTimestamp = timestamps[1];
            graphArray = jsonHandler.getGraph(url, startTimestamp, endTimestamp, depth);
        } else {
            graphArray = jsonHandler.getGraph(url, timestamp, depth);
        }

        HashSet<String> uniqueGraphArray = new HashSet<>();
        uniqueGraphArray.addAll(graphArray);

        return formulateResponse(new ArrayList<>(uniqueGraphArray), "\n");
    }

    public String getVersions(String url, String dateTime) {
        jsonHandler = new JSONHandler(false);
        return formulateResponse(jsonHandler.getVersions(url, dateTime), ",");
    }

    public String getLatestVersion(String url) {
        jsonHandler = new JSONHandler(false);
        return formulateResponse(jsonHandler.getLatestVersion(url), "\n");
    }

    private String formulateResponse(ArrayList<String> stringResponse, String delimiter) {
        String response = stringResponse.remove(0);
        for (String responseObject : stringResponse) {
            response += delimiter + responseObject;
        }
        if (response.isEmpty()) {
            LOGGER.info("No Match Found");
        } else {
            LOGGER.debug("Graph Returned: " + response);
            LOGGER.info("Returned Match Successfully");
        }
        return response;
    }

    public String updateGraph(String jsonGraph, String workspaceName) {
        boolean done, multipleURLs = false;
        String url = "";
        String timestamp = "";
        LOGGER.info("Update Graph");

        if (workspaceName.equals("*"))
            multipleURLs = true;
        else {
            Map<String, String> workspaceNameParameters = workspaceNameHandler.splitWorkspaceName(workspaceName);
            if (workspaceNameParameters == null)
                return PropertiesHandler.getProperty("badRequestResponseStatus");
            url = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceURL"));
            timestamp = workspaceNameParameters.get(PropertiesHandler.getProperty("workspaceTimestamp"));
        }

        jsonHandler = new JSONHandler(multipleURLs);
        if (!jsonGraph.startsWith("{") && jsonGraph.contains("&")) {
            jsonGraph = jsonGraph.split("&")[1];
        }
        // Gephi uses \\r as splitter between json lines
        String[] jsonLines = jsonGraph.replace("\n", "").split("\\r");
        for (String jsonLine : jsonLines) {
            if (!jsonLine.equals("")) {
                done = jsonHandler.addNodesAndEdgesFromJSONLine(jsonLine, url, timestamp);
                if (!done)
                    return PropertiesHandler.getProperty("badRequestResponseStatus");
            }
        }
        done = neo4jHandler.addNodesAndRelationships(jsonHandler.getGraphNodes(), jsonHandler.getGraphEdges());
        if (done) {
            jsonGraph = jsonGraph.replace("=", "");
            jsonGraph = jsonGraph.replace("\\r", "");

            LOGGER.info("Graph Updated Successfully");
            LOGGER.debug("JSON Data: " + jsonGraph);
            return jsonGraph + "\n";
        } else
            LOGGER.info("Could not Update Graph");
        LOGGER.debug("JSON Data: " + jsonGraph);
        return "";
    }
}
