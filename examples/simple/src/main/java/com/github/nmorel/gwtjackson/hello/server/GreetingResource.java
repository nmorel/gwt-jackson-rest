package com.github.nmorel.gwtjackson.hello.server;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.github.nmorel.gwtjackson.hello.shared.FieldVerifier;
import com.github.nmorel.gwtjackson.hello.shared.GreetingRequest;
import com.github.nmorel.gwtjackson.hello.shared.GreetingResponse;
import com.github.nmorel.gwtjackson.rest.processor.GenRestBuilder;

/**
 * @author Nicolas Morel
 */
@GenRestBuilder
@Path( "hello" )
public class GreetingResource {

    @Inject
    private HttpServletRequest httpServletRequest;

    @GET
    public String hello( @QueryParam( "name" ) String name ) {
        return "Hello " + name + "!";
    }

    @POST
    @Produces( "application/json" )
    @Consumes( "application/json" )
    public GreetingResponse greet( GreetingRequest request ) {
        // Verify that the input is valid.
        if ( !FieldVerifier.isValidName( request.getName() ) ) {
            // If the input is not valid, throw an IllegalArgumentException back to
            // the client.
            throw new IllegalArgumentException( "Name must be at least 4 characters long" );
        }

        GreetingResponse response = new GreetingResponse();
        response.setServerInfo( httpServletRequest.getServletContext().getServerInfo() );
        response.setUserAgent( httpServletRequest.getHeader( "User-Agent" ) );
        response.setGreeting( "Hello, " + request.getName() + "!" );

        return response;
    }

}
