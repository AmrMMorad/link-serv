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
    private JSONHandler jsonHandler = new JSONHandler();

    private static final Logger LOGGER = LogManager.getLogger(LinkServService.class);

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
        jsonHandler.initialize(multipleURLs);
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

            LOGGER.info("Graph has been successfully updated");
            LOGGER.debug("JSON Data: " + jsonGraph);
            return jsonGraph + "\n";
        } else
            LOGGER.info("Could not Update Graph");
        LOGGER.debug("JSON Data: " + jsonGraph);
        return "";
    }

    public String getGraph(String workspaceName, Integer depth) {

        String timeRangeDelimiter = PropertiesHandler.getProperty("timeRangeDelimiter");
        jsonHandler.initialize(false);

        ArrayList<String> graphArray;

        Map<String, String> workspaceNameParameters = workspaceNameHandler.splitWorkspaceName(workspaceName);

        if (workspaceNameParameters == null) {
            LOGGER.error("Response Status: 400 - Bad Request");
            LOGGER.error("Invalid workspace name parameters");
            return PropertiesHandler.getProperty("badRequestResponseStatus");
        }

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

        return formulateResponse(graphArray, "\r\n");
    }

    public String getVersions(String url, String dateTime) {
        jsonHandler.initialize(false);
        ArrayList<String> responseStringArray = jsonHandler.getVersions(url, dateTime);
        return formulateResponse(responseStringArray, ",");
    }

    public String getLatestVersion(String url) {
        jsonHandler.initialize(false);
        ArrayList<String> reponseStringArray = jsonHandler.getLatestVersion(url);
        return formulateResponse(reponseStringArray, "\r\n");
    }

    public String getVersionCountYearly(String url) {
        jsonHandler.initialize(false);
        String response = jsonHandler.getVersionCountYearly(url);
        LOGGER.info("Reponse: " + response);
        return response;
    }

    public String getVersionCountMonthly(String url, int year) {
        jsonHandler.initialize(false);
        String response = jsonHandler.getVersionCountMonthly(url, year);
        LOGGER.info("Reponse: " + response);
        return response;
    }

    public String getVersionCountDaily(String url, int year, int month) {
        jsonHandler.initialize(false);
        String response = jsonHandler.getVersionCountDaily(url, year, month);
        LOGGER.info("Reponse: " + response);
        return response;
    }

    private String formulateResponse(ArrayList<String> stringResponse, String delimiter) {
        HashSet<String> uniqueGraphHashSet = new HashSet<>();
        uniqueGraphHashSet.addAll(stringResponse);
        ArrayList<String> uniqueGraphArray = new ArrayList<>(uniqueGraphHashSet);

        String response = uniqueGraphArray.remove(0);
        for (String responseObject : uniqueGraphArray) {
            response += delimiter + responseObject;
        }
        if (response.isEmpty()) {
            LOGGER.info("No match found");
        } else {
            LOGGER.debug("Graph returned: " + response);
            LOGGER.info("Match has been successfully returned");
        }
        return response;
    }
}
