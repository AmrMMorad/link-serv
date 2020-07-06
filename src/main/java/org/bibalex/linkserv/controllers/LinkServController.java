package org.bibalex.linkserv.controllers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.bibalex.linkserv.errors.OperationNotFoundException;
import org.bibalex.linkserv.handlers.PropertiesHandler;
import org.bibalex.linkserv.handlers.WorkspaceNameHandler;
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
        String[] urlParams = requestURL.split(PropertiesHandler.getProperty("repositoryIP"));
        String workspaceName = ((urlParams.length == 1) ? "*" : urlParams[1]);
        LOGGER.info("Updating Graph with Parameters: " + workspaceName);
        if (operation.equals(PropertiesHandler.getProperty("updateGraph"))) {
            String response = linkServService.updateGraph(jsonGraph, workspaceName);
            if (response.equals(PropertiesHandler.getProperty("badRequestResponseStatus")))
                return ResponseEntity.badRequest().body("Please, Send only one VersionNode with timestamp and URL" +
                        " typical to those present in the request body.");
            LOGGER.info("Response Status: 200");
            return ResponseEntity.ok(response);
        } else {
            LOGGER.error("Response Status: 500, Operation Not Found: " + operation);
            throw new OperationNotFoundException(operation);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/**")
    public ResponseEntity<String> getOperations(HttpServletRequest request,
                                                @RequestParam String operation,
                                                @RequestParam(required = false, defaultValue = "1") Integer depth,
                                                @RequestParam(required = false) Integer year,
                                                @RequestParam(required = false) Integer month,
                                                @RequestParam(required = false) String dateTime) {

        PropertiesHandler.initializeProperties();
        WorkspaceNameHandler workspaceNameHandler = new WorkspaceNameHandler();
        String requestURL = request.getRequestURL().toString();
        String[] urlParams = requestURL.split(PropertiesHandler.getProperty("repositoryIP"));

        if (urlParams.length < 2) {
            return ResponseEntity.badRequest().body("Please, send a valid URL");
        }

        String workspaceName = urlParams[1];
        switch (operation) {

            case "getGraph":
                String response = linkServService.getGraph(workspaceName, depth);
                if (response.equals(PropertiesHandler.getProperty("badRequestResponseStatus")))
                    return ResponseEntity.badRequest().body("Please, send a valid URL");
                LOGGER.info("Response Status: 200");
                return ResponseEntity.ok(response);

            case "getVersionCountYearly":
                if (workspaceNameHandler.validateURL(workspaceName)) {
                    return ResponseEntity.ok(linkServService.getVersionCountYearly(workspaceName));
                }
                return ResponseEntity.badRequest().body("Please, send a valid URL");

            case "getVersionCountMonthly":
                if (year == null || !(workspaceNameHandler.validateURL(workspaceName))) {
                    return ResponseEntity.badRequest().body("Please, send a valid URL and year");
                }
                return ResponseEntity.ok(linkServService.getVersionCountMonthly(workspaceName, year));

            case "getVersionCountDaily":
                if (year == null || month == null || month < 1 || month > 12
                        || !(workspaceNameHandler.validateURL(workspaceName))) {
                    return ResponseEntity.badRequest().body("Please, send a valid URL, year and month");
                }
                return ResponseEntity.ok(linkServService.getVersionCountDaily(workspaceName, year, month));

            case "getVersions":
                if (dateTime == null || !(dateTime.matches("[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])"))
                        || !(workspaceNameHandler.validateURL(workspaceName))) {
                    return ResponseEntity.badRequest().body("Please, send a valid URL and date-time");
                }
                return ResponseEntity.ok(linkServService.getVersions(workspaceName, dateTime));

            case "getLatestVersion":
                if (workspaceNameHandler.validateURL(workspaceName)) {
                    return ResponseEntity.ok(linkServService.getLatestVersion(workspaceName));
                }
                return ResponseEntity.badRequest().body("Please, send a valid URL");

            default:
                LOGGER.error("Response Status: 500, Operation Not Found: " + operation);
                throw new OperationNotFoundException(operation);
        }
    }
}