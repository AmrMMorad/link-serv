package org.bibalex.linkserv.errors;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String operation) {
        super("Invalid operation: " + operation);
    }

}