package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseEndpointTest {

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testSimpleCheckingArchivingWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001 (id 1):
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }

  @Test
  public void testCreateWarehouseSuccessfullyAndGetById() {
    final String path = "warehouse";

    String newWarehouseJson = """
        {
          "businessUnitCode": "MWH.099",
          "location": "HELMOND-001",
          "capacity": 40,
          "stock": 15
        }
        """;

    given()
        .contentType("application/json")
        .body(newWarehouseJson)
        .when()
        .post(path)
        .then()
        .statusCode(201)
        .body(containsString("MWH.099"), containsString("HELMOND-001"));

    given()
        .when()
        .get(path + "/MWH.099")
        .then()
        .statusCode(200)
        .body(containsString("MWH.099"), containsString("HELMOND-001"));
  }

  @Test
  public void testCreateWarehouseWithInvalidLocationReturns400() {
    final String path = "warehouse";

    String invalidWarehouse = """
        {
          "businessUnitCode": "MWH.088",
          "location": "INVALID-LOC",
          "capacity": 40,
          "stock": 15
        }
        """;

    given()
        .contentType("application/json")
        .body(invalidWarehouse)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceWarehouseSuccessfully() {
    final String path = "warehouse";

    String replacementJson = """
        {
          "businessUnitCode": "MWH.012",
          "location": "AMSTERDAM-001",
          "capacity": 60,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(replacementJson)
        .when()
        .post(path + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("60"));
  }

  @Test
  public void testReplaceWarehouseWithMismatchedStockReturns400() {
    final String path = "warehouse";

    String replacementJson = """
        {
          "businessUnitCode": "MWH.023",
          "location": "TILBURG-001",
          "capacity": 35,
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(replacementJson)
        .when()
        .post(path + "/MWH.023/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testGetNonExistentWarehouseReturns404() {
    given()
        .when()
        .get("warehouse/NON_EXISTENT_ID")
        .then()
        .statusCode(404);
  }

  @Test
  public void testReplaceNonExistentWarehouseReturns404() {
    String replacementJson = """
        {
          "businessUnitCode": "NON_EXISTENT_BU",
          "location": "AMSTERDAM-001",
          "capacity": 60,
          "stock": 5
        }
        """;

    given()
        .contentType("application/json")
        .body(replacementJson)
        .when()
        .post("warehouse/NON_EXISTENT_BU/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void testReplaceWarehouseCapacityCannotAccommodateStockReturns400() {
    // MWH.023 has stock 27
    String replacementJson = """
        {
          "businessUnitCode": "MWH.023",
          "location": "TILBURG-001",
          "capacity": 20,
          "stock": 27
        }
        """;

    given()
        .contentType("application/json")
        .body(replacementJson)
        .when()
        .post("warehouse/MWH.023/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseDuplicateBuCodeReturns400() {
    String json = """
        {
          "businessUnitCode": "MWH.023",
          "location": "TILBURG-001",
          "capacity": 35,
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseStockExceedsCapacityReturns400() {
    String json = """
        {
          "businessUnitCode": "MWH.777",
          "location": "TILBURG-001",
          "capacity": 20,
          "stock": 25
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testArchiveNonExistentWarehouseReturns404() {
    given()
        .when()
        .delete("warehouse/99999")
        .then()
        .statusCode(404);
  }
}
