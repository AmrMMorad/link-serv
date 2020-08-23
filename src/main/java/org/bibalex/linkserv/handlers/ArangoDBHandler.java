package org.bibalex.linkserv.handlers;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.GraphCreateOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bibalex.linkserv.models.Edge;
import org.bibalex.linkserv.models.HistogramEntry;
import org.bibalex.linkserv.models.Node;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.*;

import static org.neo4j.driver.Values.parameters;

public class ArangoDBHandler {

    private static final Logger LOGGER = LogManager.getLogger(ArangoDBHandler.class);
    private String versionNodeLabel;
    private String parentNodeLabel;
    private String linkRelationshipType;
    private Session session;
    private String query;
    private Value parameterValues;

    private ArangoDatabase linkData = getArangoDataBase();
    private ArangoGraph linkDataGraph = linkData.graph("link-data-graph");

    public ArangoDBHandler() {
        this.versionNodeLabel = PropertiesHandler.getProperty("versionNodeLabel");
        this.parentNodeLabel = PropertiesHandler.getProperty("parentNodeLabel");
        this.linkRelationshipType = PropertiesHandler.getProperty("linkRelationshipType");
    }

    public Session getSession() {
        if (session == null || !session.isOpen()) {
            Driver driver = GraphDatabase.driver(PropertiesHandler.getProperty("uri"));
            session = driver.session();
        }
        return session;
    }

    private ArangoDatabase getArangoDataBase() {
        ArangoDB arangoDB = new ArangoDB.Builder().user("root").password("root").build();
        // check if link-data is present, and create one if not
        linkData = arangoDB.db("link-data").exists() ? arangoDB.db("link-data") : createLinkDataGraph(arangoDB);
        return linkData;
    }

    private ArangoDatabase createLinkDataGraph(ArangoDB arangoDB) {
        arangoDB.db("link-data").create();
        linkData = arangoDB.db("link-data");
        EdgeDefinition linkedToEdgeDefinition = new EdgeDefinition()
                .collection("LinkedTo")
                .from("Nodes")
                .to("Nodes");
        linkData.createGraph("link-data-graph",
                new ArrayList<>(Arrays.asList(new EdgeDefinition[] {linkedToEdgeDefinition})),
                new GraphCreateOptions().numberOfShards(9).smartGraphAttribute("url" ));
        linkDataGraph = linkData.graph("link-data-graph");
        return linkData;
    }

    public boolean addVerticesAndEdges(Map<String, Node> graphNodes, ArrayList<Edge> graphEdges) {
        return true;
    }

    //get the root node of the exact given timestamp
    public ArrayList<Node> getRootNode(String url, String timestamp) {
        LOGGER.info("Getting root node of URL: " + url + " with timestamp: " + timestamp);
        parameterValues = parameters("version", timestamp, "url", url);

        query = "CALL linkserv." + PropertiesHandler.getProperty("getRootNodeProcedure") + "($url, $version);";

        return runGetNodeQuery(query, parameterValues);
    }

    //get the root nodes matching a given range of timestamps
    public ArrayList<Node> getRootNodes(String url, String startTimestamp, String endTimestamp) {
        LOGGER.info("Getting root nodes of URL: " +
                url + " within the time range: [" + startTimestamp + ", " + endTimestamp + "]");
        parameterValues = parameters("url", url,
                "startTimestamp", startTimestamp,
                "endTimestamp", endTimestamp);

        query = "CALL linkserv." + PropertiesHandler.getProperty("getRootNodesProcedure") +
                "($url, $startTimestamp, $endTimestamp);";

        return runGetNodeQuery(query, parameterValues);
    }

    public ArrayList<Node> getVersions(String url, String dateTime) {
        LOGGER.info("Getting versions of URL: " + url + " on: " + dateTime);
        parameterValues = parameters("url", url, "dateTime", dateTime);
        query = "CALL linkserv." + PropertiesHandler.getProperty("getVersionsProcedure") +
                "($url, $dateTime);";

        return runGetNodeQuery(query, parameterValues);
    }

    public ArrayList<Node> getLatestVersion(String url) {
        LOGGER.info("Getting the latest version of URL: " + url);
        parameterValues = parameters("url", url);
        query = "CALL linkserv." + PropertiesHandler.getProperty("getLatestVersionProcedure") +
                "($url);";

        return runGetNodeQuery(query, parameterValues);
    }

    private ArrayList<Node> runGetNodeQuery(String query, Value parameterValues) {
        ArrayList<Node> resultNodes = new ArrayList<>();
        Node resultNode;

        Result result = getSession().run(query, parameterValues);
        while (result.hasNext()) {
            Record rootNodeRecord = result.next();
            resultNode = new Node(convertValueToString(rootNodeRecord.get("nodeId")),
                    versionNodeLabel,
                    convertValueToString(rootNodeRecord.get("nodeURL")),
                    convertValueToString(rootNodeRecord.get("versionName")));
            resultNodes.add(resultNode);
        }
        return resultNodes;
    }

    // get the closest version to the rootNodeVersion, we'll just assume they're the same for now
    public ArrayList<Object> getOutlinkNodes(String nodeName, String nodeVersion) {

        LOGGER.info("Getting outlinks of URL: " + nodeName + " with timestamp: " + nodeVersion);

        ArrayList<Object> outlinkEntities = new ArrayList();
        parameterValues = parameters("name", nodeName, "version", nodeVersion);

        query = "CALL linkserv." + PropertiesHandler.getProperty("getOutlinkNodesProcedure") + "($name, $version);";

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

    public boolean addNodesAndRelationships(Map<String, Node> graphNodes, ArrayList<Edge> graphEdges) {

        LOGGER.info("Update Graph: Adding nodes and edges");
        LOGGER.debug(graphNodes);

        Map<String, ArrayList<String>> nodeWithOutlinks = new HashMap<>();

        for (Edge edge : graphEdges) {
            String sourceNodeId = edge.getSource();
            ArrayList<String> outlinks = nodeWithOutlinks.get(sourceNodeId);
            if (outlinks == null)
                outlinks = new ArrayList<>();
            outlinks.add((graphNodes.get(edge.getTarget())).getUrl());
            graphNodes.remove(edge.getTarget());
            nodeWithOutlinks.put(sourceNodeId, outlinks);
        }
        boolean result = true;
        for (Map.Entry<String, ArrayList<String>> entry : nodeWithOutlinks.entrySet()) {
            if (!result) {
                LOGGER.info("Could not update graph");
                return false;
            }
            result = addOneNodeWithItsOutlinks(entry, graphNodes);
        }
        // if there are nodes not connected to edges
        for (Map.Entry<String, Node> entry : graphNodes.entrySet()) {
            if (!result) {
                LOGGER.info("Could not update graph");
                return false;
            }
            result = addNodewithItsVersion(entry);
        }
        LOGGER.info("Graph has been successfully updated");
        return true;
    }

    public ArrayList<HistogramEntry> getVersionCountYearly(String url) {
        LOGGER.info("Getting version count per year for URL: " + url);
        parameterValues = parameters("url", url);
        query = "CALL linkserv." + PropertiesHandler.getProperty("getVersionCountYearlyProcedure") + "($url)";
        return getNeo4jResultAndCovertToHistogram(parameterValues, query);
    }

    public ArrayList<HistogramEntry> getVersionCountMonthly(String url, int year) {
        LOGGER.info("Getting version count per month for URL: " + url + " in the year: " + year);
        parameterValues = parameters("url", url, "year", year);
        query = "CALL linkserv." + PropertiesHandler.getProperty("getVersionCountMonthlyProcedure") + "($url,$year)";
        return getNeo4jResultAndCovertToHistogram(parameterValues, query);
    }

    public ArrayList<HistogramEntry> getVersionCountDaily(String url, int year, int month) {
        LOGGER.info("Getting version count per day for URL: " + url + " in year: " + year + " and month: " + month);
        parameterValues = parameters("url", url, "year", year, "month", month);
        query = "CALL linkserv." + PropertiesHandler.getProperty("getVersionCountDailyProcedure") + "($url,$year,$month)";
        return getNeo4jResultAndCovertToHistogram(parameterValues, query);
    }

    private List<Map<String, Object>> convertArrayToJSONArray(ArrayList<String> outlinks) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String url : outlinks) {
            Map<String, Object> stringObjectMap = new HashMap<>();
            stringObjectMap.put("url", url);
            list.add(stringObjectMap);
        }
        return list;
    }

    private boolean neo4jAddNodeWithOutlinks(String url, String timestamp, ArrayList<String> outlinks) {
        System.out.println("&&&&&&&&&&&&&&&&&\n" + url);
        System.out.println(timestamp);
        BaseDocument vertex = new BaseDocument();
        vertex.addAttribute("url", url);
        vertex.addAttribute("timestamp", timestamp);
        linkDataGraph.vertexCollection("Nodes").insertVertex(vertex);
        System.out.println("!!!!!!!!!!!!!! \n ADD VERTEX !!!!!!!!!!");
        List<BaseDocument> outlinksVertices = new ArrayList<>();
        for( String outlinkURL : outlinks){
            BaseDocument outlinkVertex = new BaseDocument();
            outlinkVertex.addAttribute("url", outlinkURL);
            outlinkVertex.addAttribute("timestamp", timestamp);

        }
        return true;
    }

    private String convertValueToString(Value value) {
        return String.valueOf(value).replace("\"", "");
    }

    private boolean addOneNodeWithItsOutlinks(Map.Entry<String, ArrayList<String>> entry, Map<String, Node> graphNodes) {
        String url = (graphNodes.get(entry.getKey())).getUrl();
        String timestamp = (graphNodes.get(entry.getKey())).getTimestamp();
        graphNodes.remove(entry.getKey());
        return neo4jAddNodeWithOutlinks(url, timestamp, entry.getValue());
    }

    private boolean addNodewithItsVersion(Map.Entry<String, Node> entry) {
        String url = entry.getValue().getUrl();
        String timestamp = entry.getValue().getTimestamp();
        //as it is useless to add node without version and edges
        if (timestamp != null) {
            return neo4jAddNodeWithOutlinks(url, timestamp, new ArrayList<String>());
        }
        return true;
    }

    private ArrayList<HistogramEntry> getNeo4jResultAndCovertToHistogram(Value parameters, String query) {
        ArrayList<HistogramEntry> histogramEntries = new ArrayList<>();
        Result result = getSession().run(query, parameters);

        while (result.hasNext()) {
            Record histogramRecord = result.next();
            HistogramEntry histogramEntry = new HistogramEntry(histogramRecord.get("key").asInt(),
                    histogramRecord.get("count").asInt());
            histogramEntries.add(histogramEntry);
        }
        return histogramEntries;
    }
}