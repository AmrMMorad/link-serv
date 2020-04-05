package org.bibalex.linkserv.handlers;

import org.bibalex.linkserv.models.Edge;
import org.bibalex.linkserv.models.Node;
import org.json.JSONObject;

import java.util.ArrayList;

public class JSONHandler {

    private Neo4jHandler neo4jHandler = new Neo4jHandler();
    private ArrayList<Object> data;
    private static final int DEFAULT_ATTRIBUTE_VALUE = 1;

    public JSONHandler(){
        this.data= new ArrayList<>();
    }

    public ArrayList<Object> getProperties(String jsonLine){

        JSONObject jsonData = new JSONObject(jsonLine);
        if(!jsonData.isNull(PropertiesHandler.getProperty("addNodeKey"))){
            handleNode(jsonData);
        }
        else{
            handleEdge(jsonData);
        }
        return data;
    }

    private void handleNode(JSONObject jsonData){

        JSONObject jsonNode = jsonData.getJSONObject(PropertiesHandler.getProperty("addNodeKey"));
        String nodeId = jsonNode.keys().next();
        JSONObject JsonNodeProperties = jsonNode.getJSONObject(nodeId);
        if(JsonNodeProperties.isNull(PropertiesHandler.getProperty("versionKey"))) {
            Node node = new Node(nodeId,PropertiesHandler.getProperty("parentNodeLabel"),
                    JsonNodeProperties.getString(PropertiesHandler.getProperty("labelKey")), null);
            data.add(node);
        }
        else{
            Node node = new Node(nodeId, PropertiesHandler.getProperty("versionNodeLabel"),
                    JsonNodeProperties.getString(PropertiesHandler.getProperty("labelKey")),
                    JsonNodeProperties.getString(PropertiesHandler.getProperty("versionKey")));
            data.add(node);
        }
    }

    private void handleEdge(JSONObject jsonData){

        JSONObject jsonEdge = jsonData.getJSONObject(PropertiesHandler.getProperty("addEdgeKey"));
        String edgeId = jsonEdge.keys().next();
        JSONObject JsonEdgeProperties = jsonEdge.getJSONObject(edgeId);
        Edge edge = new Edge(edgeId, PropertiesHandler.getProperty("linkRelationshipType"),
                JsonEdgeProperties.getString(PropertiesHandler.getProperty("sourceKey")),
                JsonEdgeProperties.getString(PropertiesHandler.getProperty("targetKey")));
//        data.add(edge);
    }

    public ArrayList<JSONObject> getGraph(String url, String timestamp, Integer depth) {

        ArrayList<JSONObject> results = new ArrayList<>();
        Node rootNode = neo4jHandler.getRootNode(url, timestamp);

        // validate presence of all attributes before proceeding to the next level nodes
        if (rootNode.equals(null) || depth < 1) {
            results.add(new JSONObject());
            return results;
        }

        /** get the actual timestamp of the returned root node in case of approximation later on,
         where the given timestamp of the request is not necessarily equal to the actual one returned.**/
        String nodeName = rootNode.getUrl();
        String nodeVersion = rootNode.getTimestamp();

        results.add(addNodeToResults(rootNode));

        for(int i = 0; i < depth; i++){
            ArrayList<Object> outlinkNodes = neo4jHandler.getOutlinkNodes(nodeName, nodeVersion);
            for(Object nodeMap: outlinkNodes){
                if(nodeMap.getClass() == Node.class) {
                    results.add(addNodeToResults((Node) nodeMap));
                    nodeName = ((Node) nodeMap).getUrl();
                }
                else
                    results.add(addEdgeToResults((Edge) nodeMap));
            }
        }
        return results;
    }

    private JSONObject addNodeToResults(Node node) {

        JSONObject outlinkNodeData = new JSONObject();
        outlinkNodeData.put(PropertiesHandler.getProperty("nameKey"), node.getUrl());
        outlinkNodeData.put(PropertiesHandler.getProperty("versionKey"), node.getTimestamp());
        outlinkNodeData.put(PropertiesHandler.getProperty("typeKey"), node.getType());
        outlinkNodeData = setDefaultNodeAttributes(outlinkNodeData);

        JSONObject fullNode = new JSONObject();
        String nodeID = node.getId();
        fullNode.put(nodeID, outlinkNodeData);

        JSONObject nodeJSON = new JSONObject();
        nodeJSON.put(PropertiesHandler.getProperty("addNodeKey"), fullNode);

        return nodeJSON;
    }

    private JSONObject addEdgeToResults(Edge edge) {

        JSONObject outlinkEdgeData = new JSONObject();

        outlinkEdgeData.put(PropertiesHandler.getProperty("sourceKey"), edge.getSource());
        outlinkEdgeData.put(PropertiesHandler.getProperty("targetKey"), edge.getTarget());
        outlinkEdgeData.put(PropertiesHandler.getProperty("typeKey"), edge.getType());
        outlinkEdgeData.put("directed", true);
        outlinkEdgeData.put("r", DEFAULT_ATTRIBUTE_VALUE);
        outlinkEdgeData.put("g", DEFAULT_ATTRIBUTE_VALUE);
        outlinkEdgeData.put("b", DEFAULT_ATTRIBUTE_VALUE);
        outlinkEdgeData.put("weight", DEFAULT_ATTRIBUTE_VALUE);

        JSONObject fullEdge = new JSONObject();
        fullEdge.put(edge.getId(), outlinkEdgeData);

        JSONObject edgeJSON = new JSONObject();
        edgeJSON.put(PropertiesHandler.getProperty("addEdgeKey"), fullEdge);

        return edgeJSON;
    }

    private JSONObject setDefaultNodeAttributes(JSONObject nodeData) {

        String[] attributes = new String[]{"r", "g", "b", "x", "y", "z", "size"};

        for(String attribute : attributes) {
            nodeData.put(attribute, DEFAULT_ATTRIBUTE_VALUE);
        }
        return nodeData;
    }

    public ArrayList<Object> getData() {
        return data;
    }

    public void setData(ArrayList<Object> data) {
        this.data = data;
    }
}
