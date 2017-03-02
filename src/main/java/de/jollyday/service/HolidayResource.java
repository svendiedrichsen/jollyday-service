package de.jollyday.service;

import de.jollyday.*;

import javax.json.*;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Path("calendars/{calendar}")
public class HolidayResource {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.mm.yyyy");

    @Context
    private HttpServletRequest request;

    @GET
    @Path("/holidays")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHolidaysWithoutPath(@PathParam("calendar") String calendar, @QueryParam("year") Integer year, @QueryParam("from") String fromStr, @QueryParam("until") String untilStr){
        return getHolidaysWithPath(Collections.emptyList(), calendar, year, fromStr, untilStr);
    }

    @GET
    @Path("/{segments: .*}/holidays")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHolidaysWithPath(@PathParam("segments") List<PathSegment> segments, @PathParam("calendar") String calendar, @QueryParam("year") Integer year, @QueryParam("from") String fromStr, @QueryParam("until") String untilStr){
        final LocalDate from = fromStr != null ? LocalDate.parse(fromStr, formatter) : null;
        final LocalDate until = untilStr != null ? LocalDate.parse(untilStr, formatter) : null;
        validateDateRange(from, until);
        final ManagerParameter managerParameter = ManagerParameters.create(calendar);
        final HolidayManager holidayManager = HolidayManager.getInstance(managerParameter);
        Set<Holiday> holidays;
        if(from != null && until != null){
            holidays = holidayManager.getHolidays(from, until);
        } else {
            if(year == null){
                year = LocalDate.now().getYear();
            }
            holidays = holidayManager.getHolidays(year);
        }
        return Response.ok(createJsonArray(holidays)).build();
    }

    private void validateDateRange(LocalDate from, LocalDate until) {
        if((from != null && until == null) || (from == null && until != null)) {
            throw new WebApplicationException("From and until parameters both need to be provided.", Response.Status.BAD_REQUEST);
        }
        if (from != null && until != null && until.isBefore(from)){
            throw new WebApplicationException("From must be before until parameter.", Response.Status.BAD_REQUEST);
        }
    }

    private JsonArray createJsonArray(Collection<Holiday> holidays){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(Holiday holiday : holidays){
                arrayBuilder.add(createJsonObject(holiday, messageDigest));
            }
            return arrayBuilder.build();
        } catch (NoSuchAlgorithmException e) {
            throw new WebApplicationException("A problem processing holidays.", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonObject createJsonObject(Holiday holiday, MessageDigest messageDigest){
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("id", DatatypeConverter.printHexBinary(messageDigest.digest(holiday.getPropertiesKey().getBytes())));
        objectBuilder.add("date", holiday.getDate().toString());
        objectBuilder.add("type", holiday.getType().name());
        objectBuilder.add("description", holiday.getDescription(request.getLocale()));
        return objectBuilder.build();
    }

    @GET
    @Path("structure")
    @Produces(MediaType.APPLICATION_JSON+";charset=UTF-8")
    public Response getStructure(@PathParam("calendar") String calendar){
        try {
            final ManagerParameter managerParameter = ManagerParameters.create(calendar);
            final CalendarHierarchy calendarHierarchy = HolidayManager.getInstance(managerParameter).getCalendarHierarchy();
            return Response.ok(createJsonObject(calendarHierarchy)).build();
        } catch (IllegalStateException e){
            throw new WebApplicationException("Calendar '"+calendar+"' not found.", Response.Status.NOT_FOUND);
        }
    }

    private JsonObject createJsonObject(CalendarHierarchy calendarHierarchy){
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        return createJsonObject(calendarHierarchy, objectBuilder);
    }

    private JsonObject createJsonObject(CalendarHierarchy calendarHierarchy, JsonObjectBuilder objectBuilder) {
        objectBuilder.add("id", calendarHierarchy.getId());
        objectBuilder.add("description", calendarHierarchy.getDescription(request.getLocale()));
        if(!calendarHierarchy.getChildren().isEmpty()){
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for(Map.Entry<String, CalendarHierarchy> child : calendarHierarchy.getChildren().entrySet()){
                arrayBuilder.add(createJsonObject(child.getValue()));
            }
            objectBuilder.add("children", arrayBuilder);
        }
        return objectBuilder.build();
    }

    private static String[] getSegments(List<PathSegment> pathSegments){
        List<String> segments = new ArrayList<>();
        if(pathSegments != null && !pathSegments.isEmpty()){
            for(PathSegment pathSegment : pathSegments){
                segments.add(pathSegment.getPath());
            }
        }
        return segments.toArray(new String[segments.size()]);
    }

}
