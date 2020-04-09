package org.bibalex.linkserv.handlers;

import org.bibalex.linkserv.models.Edge;
import org.bibalex.linkserv.models.Node;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;

import static org.neo4j.driver.Values.parameters;

public class Neo4jHandler {

    private String versionNodeLabel = PropertiesHandler.getProperty("versionNodeLabel");
    private String parentNodeLabel = PropertiesHandler.getProperty("parentNodeLabel");
    private String linkRelationshipType = PropertiesHandler.getProperty("linkRelationshipType");

    private Session session;

    public Session getSession() {
        if (session == null || !session.isOpen()) {
            Driver driver = GraphDatabase.driver( PropertiesHandler.getProperty("uri"));
            session = driver.session();
        }

        return session;
    }

    // v.version is returned by the query in case of approximation later on
    public Node getRootNode(String url, String timestamp) {

        Node rootNode = null;
        Value parameterValues = parameters("version", timestamp, "url", url);

        String query = "CALL linkserv.getRootNode($url, $version);";

        Result result = getSession().run(query, parameterValues);

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

        ArrayList<Object> outlinkEntities = new ArrayList();
        Value parameterValues = parameters("name", nodeName, "version", nodeVersion);

        String query = "CALL linkserv.getOutlinkNodes($name, $version);";

        Result result = getSession().run(query, parameterValues);

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

        ArrayList<String> outlinks = new ArrayList<String>();
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
        query = "CALL linkserv.addNodesAndRelationships($url,$timestamp,$outlinks)";

        Result result = getSession().run(query, parameters);

        if (result.hasNext()) {
            return true;
        } else {
            return false;
        }
    }
}