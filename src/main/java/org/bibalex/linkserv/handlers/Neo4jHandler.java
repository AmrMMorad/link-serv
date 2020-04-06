package org.bibalex.linkserv.handlers;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.bibalex.linkserv.models.Edge;
import org.bibalex.linkserv.models.Node;
import org.neo4j.driver.v1.*;

import java.util.ArrayList;

import static org.neo4j.driver.v1.Values.parameters;

public class Neo4jHandler {

    private String versionNodeLabel = PropertiesHandler.getProperty("versionNodeLabel");
    private String parentNodeLabel = PropertiesHandler.getProperty("parentNodeLabel");
    private String linkRelationshipType = PropertiesHandler.getProperty("linkRelationshipType");

    private Session session;

    private static final Logger LOGGER = LogManager.getLogger(Neo4jHandler.class);

    public Session getSession() {
        if (session == null || !session.isOpen()) {
            Driver driver = GraphDatabase.driver( PropertiesHandler.getProperty("uri"));
            session = driver.session();
        }

        return session;
    }

    // v.version is returned by the query in case of approximation later on
    public Node getRootNode(String url, String timestamp) {

        LOGGER.info("Getting Root Node of URL: " + url + " with Timestamp: " + timestamp);

        Node rootNode = null;
        Value parameterValues = parameters("version", timestamp, "url", url);

        String query = "CALL linkserv.getRootNode({url}, {version});";

        StatementResult result = getSession().run(query, parameterValues);

        while (result.hasNext()) {
            Record rootNodeRecord = result.next();
            rootNode = new Node(convertValueToString(rootNodeRecord.get("nodeId")),
                    versionNodeLabel,
                    convertValueToString(rootNodeRecord.get("parentName")),
                    convertValueToString(rootNodeRecord.get("versionName")));
        }
        return rootNode;
    }

    // get the closest version to the rootNodeVersion, we'll just assume they're the same for now
    public ArrayList<Object> getOutlinkNodes(String nodeName, String nodeVersion) {

        LOGGER.info("Getting Outlinks of Node of URL: " + nodeName + " with Timestamp: " + nodeVersion);

        ArrayList<Object> outlinkEntities = new ArrayList();
        Value parameterValues = parameters("name", nodeName, "version", nodeVersion);

        String query = "CALL linkserv.getOutlinkNodes({name}, {version});";

        StatementResult result = getSession().run(query, parameterValues);

        while (result.hasNext()) {

            Record resultRecord = result.next();
            Boolean isParent = convertValueToString(resultRecord.get("outlinkVersion")).equalsIgnoreCase("NULL");

            Node outlinkNode = new Node(isParent ? convertValueToString(resultRecord.get("parentId")) :
                    convertValueToString(resultRecord.get("outlinkVersionId")),
                    isParent ? parentNodeLabel : versionNodeLabel,
                    convertValueToString(resultRecord.get("outlinkName")),
                    isParent ? "" : convertValueToString(resultRecord.get("outlinkVersion")));

            Edge outlinkEdge = new Edge(convertValueToString(resultRecord.get("relationshipId")),
                    linkRelationshipType,
                    convertValueToString(resultRecord.get("parentVersionId")),
                    isParent ? convertValueToString(resultRecord.get("parentId")) :
                            convertValueToString(resultRecord.get("outlinkVersionId")));

            outlinkEntities.add(outlinkNode);
            outlinkEntities.add(outlinkEdge);
        }
        return outlinkEntities;
    }

    private String convertValueToString(Value value) {
        return String.valueOf(value).replace("\"", "");
    }

    public boolean addNodesAndRelationships(ArrayList<Object> data) {

        LOGGER.info("Update Graph: Adding Nodes and Edges");
        LOGGER.debug(data);

        ArrayList<String> outlinks = new ArrayList<>();
        String url="";
        String timestamp="";
        String query;

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getClass() == Node.class) {
                Node node = (Node) data.get(i);

                if (node.getTimestamp() == null) {
                    outlinks.add(node.getUrl());
                } else {
                    url = node.getUrl();
                    timestamp = node.getTimestamp();
                }
            } else {
                // Here we will handle multiple timestamps request
            }
        }
        Value parameters = parameters("url", url, "timestamp", timestamp, "outlinks", outlinks);
        query = "CALL linkserv.addNodesAndRelationships({url},{timestamp},{outlinks})";

        StatementResult result = getSession().run(query, parameters);

        if (result.hasNext()) {
            LOGGER.info("Graph Updated Successfully");
            return true;
        } else {
            LOGGER.info("Could not Update Graph");
            return false;
        }
    }
}