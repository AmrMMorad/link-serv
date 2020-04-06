package org.bibalex.linkserv.controllers;

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

    @RequestMapping(value = "/{workspaceName}", method = RequestMethod.POST)
    public ResponseEntity<String> updateGraph(@PathVariable("workspaceName") String workspaceName,
                                              @RequestBody String jsonData,
                                              @RequestParam String operation) {
        if (operation.equals("updateGraph")) {
            return ResponseEntity.ok(linkServService.updateGraph(jsonData));
        } else {
            throw new OperationNotFoundException(operation);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{workspaceName}")
    public ResponseEntity<Object> getGraph(@PathVariable("workspaceName") String workspaceName,
                                           @RequestParam String operation,
                                           @RequestParam(required = false, defaultValue = "1") Integer depth) {

        if (operation.equals("getGraph")) {
            return ResponseEntity.ok(linkServService.getGraph(workspaceName, depth));
        } else {
            throw new OperationNotFoundException(operation);
        }

    }

}