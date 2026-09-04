package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.fulfillment.adapters.database.DbFulfillment;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentAssignment;
import com.fulfilment.application.monolith.fulfillment.domain.FulfillmentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  @Inject FulfillmentService fulfillmentService;

  @POST
  public Response assignFulfillment(FulfillmentAssignment assignment) {
    if (assignment == null) {
      throw new WebApplicationException("Payload cannot be null", 400);
    }
    try {
      DbFulfillment result = fulfillmentService.assignFulfillment(
          assignment.storeId, assignment.productId, assignment.warehouseBusinessUnitCode);
      return Response.status(201).entity(result).build();
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @GET
  @Path("store/{storeId}")
  public List<DbFulfillment> getByStore(@PathParam("storeId") Long storeId) {
    return fulfillmentService.getFulfillmentsByStore(storeId);
  }

  @GET
  @Path("warehouse/{buCode}")
  public List<DbFulfillment> getByWarehouse(@PathParam("buCode") String buCode) {
    return fulfillmentService.getFulfillmentsByWarehouse(buCode);
  }

  @DELETE
  @Path("{id}")
  public Response remove(@PathParam("id") Long id) {
    boolean deleted = fulfillmentService.removeFulfillment(id);
    if (!deleted) {
      throw new WebApplicationException("Fulfillment assignment not found", 404);
    }
    return Response.noContent().build();
  }
}
