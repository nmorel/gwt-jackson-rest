package com.github.nmorel.gwtjackson.hello.server;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.github.nmorel.gwtjackson.hello.shared.FieldVerifier;
import com.github.nmorel.gwtjackson.hello.shared.GreetingRequest;
import com.github.nmorel.gwtjackson.hello.shared.GreetingResponse;
import com.github.nmorel.gwtjackson.rest.processor.GenResponseClassType;
import com.github.nmorel.gwtjackson.rest.processor.GenRestBuilder;

/**
 * @author Nicolas Morel
 */
@GenRestBuilder
@Path( "hello" )
public class GreetingResource {

    @GET
    public GreetingResponse hello( @Context HttpServletRequest httpRequest, @QueryParam( "name" ) String name ) {
        return greet( httpRequest, new GreetingRequest( name ) );
    }

    @POST
    @Path( "/ping" )
    public void ping( @Context HttpServletRequest httpRequest ) {
    }

    @POST
    @Produces( "application/json" )
    @Consumes( "application/json" )
    public GreetingResponse greet( @Context HttpServletRequest httpRequest, GreetingRequest request ) {
        // Verify that the input is valid.
        if ( !FieldVerifier.isValidName( request.getName() ) ) {
            // If the input is not valid, throw an IllegalArgumentException back to
            // the client.
            throw new IllegalArgumentException( "Name must be at least 4 characters long" );
        }

        GreetingResponse response = new GreetingResponse();
        response.setServerInfo( httpRequest.getServletContext().getServerInfo() );
        response.setUserAgent( httpRequest.getHeader( "User-Agent" ) );
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
    public GreetingResponse greet( @Context HttpServletRequest httpRequest, @PathParam( "id" ) String id, @QueryParam( "opt" ) String
            opt, GreetingRequest request ) {
        GreetingResponse response = greet( httpRequest, request );
        response.setGreeting( "Hello #" + id + ", " + request.getName() + "!" );
        return response;
    }

    @POST
    @Path( "/{id}" )
    @Consumes( "application/json" )
    @GenResponseClassType( GreetingResponse.class )
    public Response greetWithCustomHTTPCode( @Context HttpServletRequest httpRequest, @PathParam( "id" ) String id,
                                             @QueryParam( "opt" ) String opt, GreetingRequest request ) {

        GreetingResponse response = greet( httpRequest, request );
        response.setGreeting( "Hello #" + id + ", " + request.getName() + "!" );
        return Response.ok( response ).build();
    }

}
