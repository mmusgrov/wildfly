package org.wildfly.test.integration.microprofile.lra.participant.smoke;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

import org.wildfly.test.integration.microprofile.lra.EnableLRAExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel.HotelParticipant;
import org.wildfly.test.integration.microprofile.lra.participant.smoke.model.Booking;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(EnableLRAExtensionsSetupTask.class)
public class LRAParticipantSmokeTest {

    private static final String LRA_COORDINATOR_URL_KEY = "lra.coordinator.url";
    private static final String START_PATH = "start";
    private static final String CANCEL_PATH = "/cancel";
    private static final String CLOSE_PATH = "/close";
    private static final String CLIENT_ID_PARAM_NAME = "ClientID";

    private static final long CLIENT_TIMEOUT = Long.getLong("lra.internal.client.timeout", 10);
    private static final long START_TIMEOUT = Long.getLong("lra.internal.client.timeout.start", CLIENT_TIMEOUT);
    private static final long END_TIMEOUT = Long.getLong("lra.internal.client.end.timeout", CLIENT_TIMEOUT);

    @ArquillianResource
    public URL baseURL;

    public Client client;

    @Before
    public void before() {
        System.setProperty(LRA_COORDINATOR_URL_KEY, "http://127.0.0.1:8080/lra-coordinator/lra-coordinator");
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        if (client != null) {
            client.close();
        }
    }

    @Deployment
    public static WebArchive getDeployment() {

        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "lra-participant-test.war")
                .addPackages(true,
                        "org.wildfly.test.integration.microprofile.lra.participant.smoke.hotel",
                        "org.wildfly.test.integration.microprofile.lra.participant.smoke.model")
                .addClasses(LRAParticipantSmokeTest.class,
                        EnableLRAExtensionsSetupTask.class,
                        CLIServerSetupTask.class)
                .addAsWebInfResource(LRAParticipantSmokeTest.class.getClassLoader()
                        .getResource("META-INF/web/web.xml"), "web.xml");
        return webArchive;
    }

    @Test
    public void hotelParticipantCompleteBookingTest() throws Exception {
        final URI lraId = startBooking();
        String id = getLRAUid(lraId.toString());
        closeLRA(id);
        validateBooking(id, true);
    }

    @Test
    public void hotelParticipantCompensateBookingTest() throws Exception {
        final URI lraId = startBooking();
        String id = getLRAUid(lraId.toString());
        compensateBooking(id);
        validateBooking(id, false);
    }

    private URI startBooking() throws Exception {
        Booking b = bookHotel("Paris-hotel");
        return new URI(b.getId());
    }

    private String completeBooking(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            response = client.target(baseURL.toURI())
                    .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                    .path(HotelParticipant.TRANSACTION_COMPLETE)
                    .request()
                    .header(LRA_HTTP_CONTEXT_HEADER, lraId)
                    .put(Entity.text(""));

            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

            /*Assert.assertEquals(
                    "response from " + HotelParticipant.HOTEL_PARTICIPANT_PATH + "/" + HotelParticipant.TRANSACTION_COMPLETE + " was " + response.getStatus(),
                    200, response.getStatus());*/
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private String compensateBooking(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            response = client.target(baseURL.toURI())
                    .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                    .path(HotelParticipant.TRANSACTION_COMPENSATE)
                    .request()
                    .header(LRA_HTTP_CONTEXT_HEADER, lraId)
                    .put(Entity.text(""));

            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private static final Pattern UID_REGEXP_EXTRACT_MATCHER = Pattern.compile(".*/([^/?]+).*");

    public String getLRAUid(String lraId) {
        return lraId == null ? null : UID_REGEXP_EXTRACT_MATCHER.matcher(lraId).replaceFirst("$1");
    }

    private void validateBooking(String lraId, boolean isEntryPresent) throws Exception {
        Response response = null;
        try {
            response = client.target(baseURL.toURI())
                    .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                    .request()
                    .get();

            String result = response.readEntity(String.class);
            if (isEntryPresent){
                Assert.assertTrue(
                        "Booking confirmed", result.contains("CONFIRMED"));
            } else {
                Assert.assertTrue(
                        "Booking cancelled", result.contains("CANCELLED"));
            }
        } catch (URISyntaxException e) {
            throw new Exception("Response: " + String.valueOf(response) + ", Exception Message: " + e.getMessage());
        }
    }


    private Booking bookHotel(String name) throws Exception {
        Response response = null;
        try {
            response = client.target(baseURL.toURI())
                    .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                    .queryParam("hotelName", name)
                    .request()
                    .post(Entity.text(""));

            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                response.close();
                throw new Exception("hotel booking problem");
            } else if (response.getEntity() != null ){
                String result = response.readEntity(String.class);
                ObjectMapper obj = new ObjectMapper();
                return obj.readValue(result, Booking.class);
            } else {
                throw new Exception("hotel booking problem");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    private String getLRAStatus(String lraId) throws Exception {
        URI coordinatorURI = null;
        String responseEntity = null;
        try {
            coordinatorURI = new URI(System.getProperty(LRA_COORDINATOR_URL_KEY));

            Response response = client.target(coordinatorURI)
                    .path(lraId + "/status")
                    .request()
                    .header("Narayana-LRA-API-version", "1.0")
                    .async()
                    .get()
                    .get(10, TimeUnit.SECONDS);

            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private String cancelLRA(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            URI coordinatorURI = new URI(System.getProperty(LRA_COORDINATOR_URL_KEY));
            response = client.target(coordinatorURI)
                    .path(lraId.concat(CANCEL_PATH))
                    .request()
                    .header("Narayana-LRA-API-version", "1.0")
                    .async()
                    .put(Entity.text(""))
                    .get(END_TIMEOUT, TimeUnit.SECONDS);
            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private String closeLRA(String lraId) throws Exception {
        Response response = null;
        String responseEntity = null;
        try {
            URI coordinatorURI = new URI(System.getProperty(LRA_COORDINATOR_URL_KEY));
            response = client.target(coordinatorURI)
                    .path(lraId.concat(CLOSE_PATH))
                    .request()
                    .header("Narayana-LRA-API-version", "1.0")
                    .async()
                    .put(Entity.text(""))
                    .get(END_TIMEOUT, TimeUnit.SECONDS);
            responseEntity = response.hasEntity() ? response.readEntity(String.class) : "";

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return responseEntity;
    }

    private String getAllBookings() throws Exception {
        Response response = null;
        String result = null;
        try {
            response = client.target(baseURL.toURI())
                    .path(HotelParticipant.HOTEL_PARTICIPANT_PATH)
                    .request()
                    .get();

            result = response.readEntity(String.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return result;
    }
}
