package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testGetSingleProduct() {
    given()
        .when()
        .get("product/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX"));
  }

  @Test
  public void testGetNonExistentProductReturns404() {
    given()
        .when()
        .get("product/9999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateProductSuccessfully() {
    String json = """
        {
          "name": "BILLY_BOOKSHELF",
          "description": "Bookcase white",
          "stock": 10
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .body(containsString("BILLY_BOOKSHELF"));
  }

  @Test
  public void testCreateProductWithIdThrows422() {
    String json = """
        {
          "id": 99,
          "name": "INVALID_ID_PROD"
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("product")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateProduct() {
    String json = """
        {
          "name": "KALLAX_NEW_NAME",
          "stock": 15
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .put("product/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX_NEW_NAME"));
  }

  @Test
  public void testUpdateProductWithoutNameThrows422() {
    String json = """
        {
          "stock": 15
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .put("product/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateNonExistentProductThrows404() {
    String json = """
        {
          "name": "NON_EXISTENT",
          "stock": 15
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .put("product/9999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteNonExistentProductThrows404() {
    given()
        .when()
        .delete("product/9999")
        .then()
        .statusCode(404);
  }
}
