package org.bibalex.linkserv.controllers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.bibalex.linkserv.errors.OperationNotFoundException;
import org.bibalex.linkserv.handlers.PropertiesHandler;
import org.bibalex.linkserv.services.LinkServService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class LinkServController {

    @Autowired
    private LinkServService linkServService;

    private static final Logger LOGGER = LogManager.getLogger(LinkServController.class);

    @RequestMapping(method = RequestMethod.POST, value = "/**")
    public ResponseEntity<String> updateGraph(HttpServletRequest request,
                                              @RequestBody String jsonGraph,
                                              @RequestParam String operation) {

        PropertiesHandler.initializeProperties();
        String requestURL = request.getRequestURL().toString();
        String workspaceName = requestURL.split(PropertiesHandler.getProperty("repositoryIP"))[1];
        LOGGER.info("Updating Graph with Parameters: " + workspaceName);
        if (operation.equals(PropertiesHandler.getProperty("updateGraph"))) {
            String response = linkServService.updateGraph(jsonGraph, workspaceName);
            if (response.equals(PropertiesHandler.getProperty("badRequestResponseStatus")))
                return ResponseEntity.badRequest().body("Please, Send only one VersionNode with timestamp and URL" +
                        " typical to those present in the request body");
            LOGGER.info("Response Status: 200");
            return ResponseEntity.ok(response);
        } else {
            LOGGER.error("Response Status: 500, Operation Not Found: " + operation);
            throw new OperationNotFoundException(operation);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/**")
    public ResponseEntity<String> getGraph(HttpServletRequest request,
                                           @RequestParam String operation,
                                           @RequestParam(required = false, defaultValue = "1") Integer depth) {

        PropertiesHandler.initializeProperties();
        String requestURL = request.getRequestURL().toString();
        String workspaceName = requestURL.split(PropertiesHandler.getProperty("repositoryIP"))[1];
        LOGGER.info("Getting Graph with Parameters: " + workspaceName + " and Depth: " + depth);
        if (operation.equals(PropertiesHandler.getProperty("getGraph"))) {
            String response = linkServService.getGraph(workspaceName, depth);
            if (response.equals(PropertiesHandler.getProperty("badRequestResponseStatus")))
                return ResponseEntity.badRequest().body("Please, Send a valid URL");
            LOGGER.info("Response Status: 200");
            return ResponseEntity.ok(response);
        } else {
            LOGGER.error("Response Status: 500, Operation Not Found: " + operation);
            throw new OperationNotFoundException(operation);
        }
    }
}