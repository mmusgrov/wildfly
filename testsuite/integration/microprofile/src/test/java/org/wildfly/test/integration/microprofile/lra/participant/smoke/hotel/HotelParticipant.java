package org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel;

import com.fasterxml.jackson.core.JsonProcessingException;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import org.wildfly.test.integration.microprofile.lra.participant.smoke.model.Booking;

import java.util.Collection;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Singleton
@Path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
@LRA(LRA.Type.SUPPORTS)
public class HotelParticipant {
    public static final String HOTEL_PARTICIPANT_PATH = "/hotel-participant";
    public static final String TRANSACTION_COMPLETE = "/complete";
    public static final String TRANSACTION_COMPENSATE = "/compensate";

    @Inject
    private HotelService service;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Booking bookRoom(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                            @QueryParam("hotelName") @DefaultValue("Default") String hotelName) {
        return service.book(lraId, hotelName);
    }

    @PUT
    @Path(TRANSACTION_COMPLETE)
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) throws NotFoundException, JsonProcessingException {
        service.get(lraId).setStatus(Booking.BookingStatus.CONFIRMED);
        return Response.ok(service.get(lraId).toJson()).build();
    }

    @PUT
    @Path(TRANSACTION_COMPENSATE)
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) throws NotFoundException, JsonProcessingException {
        service.get(lraId).setStatus(Booking.BookingStatus.CANCELLED);
        return Response.ok(service.get(lraId).toJson()).build();
    }

    @GET
    @Path("/{bookingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Booking getBooking(@PathParam("bookingId") String bookingId) throws JsonProcessingException {
        return service.get(bookingId);
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Booking> getAll() {
        return service.getAll();
    }
}
