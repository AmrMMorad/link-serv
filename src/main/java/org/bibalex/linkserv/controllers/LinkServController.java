package org.bibalex.linkserv.controllers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.bibalex.linkserv.errors.OperationNotFoundException;
import org.bibalex.linkserv.services.LinkServService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class LinkServController {

    @Autowired
    private LinkServService linkServService;

    private static final Logger LOGGER = LogManager.getLogger(LinkServController.class);

    @RequestMapping(value = "/{workspaceName}", method = RequestMethod.POST)
    public ResponseEntity<String> updateGraph(@PathVariable("workspaceName") String workspaceName,
                                              @RequestBody String jsonData,
                                              @RequestParam String operation) {

        LOGGER.info("Updating Graph with Parameters: " + workspaceName);
        if (operation.equals("updateGraph")) {
            LOGGER.info("Response Status: 200");
            return ResponseEntity.ok(linkServService.updateGraph(jsonData));
        } else {
            LOGGER.error("Response Status: 500, Operation Not Found: " + operation);
            throw new OperationNotFoundException(operation);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{workspaceName}")
    public ResponseEntity<String> getGraph(@PathVariable("workspaceName") String workspaceName,
                                           @RequestParam String operation,
                                           @RequestParam(required = false, defaultValue = "1") Integer depth) {

        LOGGER.info("Getting Graph with Parameters: " + workspaceName + " and Depth: " + depth);
        if (operation.equals("getGraph")) {
            LOGGER.info("Response Status: 200");
            return ResponseEntity.ok(linkServService.getGraph(workspaceName, depth));
        } else {
            LOGGER.error("Response Status: 500, Operation Not Found: " + operation);
            throw new OperationNotFoundException(operation);
        }

    }

}