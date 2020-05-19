package org.bibalex.linkserv;

import org.bibalex.linkserv.handlers.PropertiesHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.io.IOException;

@SpringBootApplication
public class MainLinkServ extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MainLinkServ.class);
    }

    public static void main (String [] args){
        SpringApplication.run(MainLinkServ.class, args);
    }
}
