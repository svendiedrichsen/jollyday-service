package de.jollyday.service;

import de.jollyday.*;

import javax.json.*;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Path("calendars/{calendar}")
public class HolidayResource {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.mm.yyyy");

    @Context
    private HttpServletRequest request;

    @GET
    @Path("holidays")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHolidays(@PathParam("calendar") String calendar, @QueryParam("from") String fromStr, @QueryParam("until") String untilStr){
        final LocalDate from = fromStr != null ? LocalDate.parse(fromStr, formatter) : null;
        final LocalDate until = untilStr != null ? LocalDate.parse(untilStr, formatter) : null;
        final ManagerParameter managerParameter = ManagerParameters.create(calendar);
        final Set<Holiday> holidays = HolidayManager.getInstance(managerParameter).getHolidays(from, until);
        return Response.ok(holidays).build();
    }

    @GET
    @Path("structure")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStructure(@PathParam("calendar") String calendar){
        try {
            final ManagerParameter managerParameter = ManagerParameters.create(calendar);
            final CalendarHierarchy calendarHierarchy = HolidayManager.getInstance(managerParameter).getCalendarHierarchy();
            return Response.ok(createJsonObject(calendarHierarchy)).build();
        } catch (IllegalStateException e){
            return Response.status(Response.Status.NOT_FOUND).entity("Calendar '"+calendar+"' not found.").build();
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
