package com.github.nmorel.gwtjackson.hello.server;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import java.io.InputStream;

import com.github.nmorel.gwtjackson.hello.shared.FieldVerifier;
import com.github.nmorel.gwtjackson.hello.shared.GreetingRequest;
import com.github.nmorel.gwtjackson.hello.shared.GreetingResponse;
import com.github.nmorel.gwtjackson.rest.processor.GenRestBuilder;
import com.github.nmorel.gwtjackson.rest.processor.GenRestIgnore;

/**
 * @author Nicolas Morel
 */
@GenRestBuilder
@Path( "hello" )
public class GreetingResource {

    @Inject
    private HttpServletRequest httpServletRequest;

    @GET
    public GreetingResponse hello( @QueryParam( "name" ) String name ) {
        return greet(new GreetingRequest( name ));
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

    //@GenRestIgnore
    //@POST
    //@Path( "/upload" )
    //@Consumes( MediaType.MULTIPART_FORM_DATA )
    //public void upload( InputStream is, GreetingRequest json ) {
    //    // do something
    //}

    @POST
    @Path( "/{id}" )
    @Produces( "application/json" )
    @Consumes( "application/json" )
    public GreetingResponse greet( @PathParam( "id" ) String id, @QueryParam( "opt" ) String opt, GreetingRequest request ) {
        GreetingResponse response = greet( request );
        response.setGreeting( "Hello #" + id + ", " + request.getName() + "!" );
        return response;
    }

}
