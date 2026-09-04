package com.fulfilment.application.monolith.fulfillment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FulfillmentEndpointTest {

  @Test
  public void testAssignFulfillmentAndQuery() {
    String json = """
        {
          "storeId": 1,
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.001"
        }
        """;

    // Create assignment
    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201)
        .body(containsString("MWH.001"));

    // Query by store
    given()
        .when()
        .get("fulfillment/store/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"));

    // Query by warehouse
    given()
        .when()
        .get("fulfillment/warehouse/MWH.001")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"));
  }

  @Test
  public void testAssignWithNonExistentStoreReturns404() {
    String json = """
        {
          "storeId": 9999,
          "productId": 1,
          "warehouseBusinessUnitCode": "MWH.001"
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("fulfillment")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteFulfillment() {
    String json = """
        {
          "storeId": 2,
          "productId": 2,
          "warehouseBusinessUnitCode": "MWH.012"
        }
        """;

    int id = given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    given()
        .when()
        .delete("fulfillment/" + id)
        .then()
        .statusCode(204);

    given()
        .when()
        .delete("fulfillment/9999")
        .then()
        .statusCode(404);
  }
}
