package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreEndpointTest {

  @Test
  public void testListStores() {
    given()
        .when()
        .get("store")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testGetSingleStore() {
    given()
        .when()
        .get("store/1")
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"));
  }

  @Test
  public void testGetNonExistentStoreReturns404() {
    given()
        .when()
        .get("store/9999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateStoreSuccessfully() {
    String json = """
        {
          "name": "NEW_STORE_XYZ",
          "quantityProductsInStock": 12
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .body(containsString("NEW_STORE_XYZ"));
  }

  @Test
  public void testCreateStoreWithIdThrows422() {
    String json = """
        {
          "id": 99,
          "name": "INVALID_STORE"
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("store")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateStore() {
    String json = """
        {
          "name": "KALLAX_UPDATED",
          "quantityProductsInStock": 20
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .put("store/2")
        .then()
        .statusCode(200)
        .body(containsString("KALLAX_UPDATED"));
  }

  @Test
  public void testPatchStore() {
    String json = """
        {
          "name": "BESTÅ_PATCHED",
          "quantityProductsInStock": 15
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .patch("store/3")
        .then()
        .statusCode(200)
        .body(containsString("BESTÅ_PATCHED"));
  }

  @Test
  public void testUpdateStoreWithoutNameThrows422() {
    String json = """
        {
          "quantityProductsInStock": 20
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .put("store/2")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateNonExistentStoreThrows404() {
    String json = """
        {
          "name": "NON_EXISTENT",
          "quantityProductsInStock": 20
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .put("store/9999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatchStoreWithoutNameThrows422() {
    String json = """
        {
          "quantityProductsInStock": 20
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .patch("store/3")
        .then()
        .statusCode(422);
  }

  @Test
  public void testPatchNonExistentStoreThrows404() {
    String json = """
        {
          "name": "NON_EXISTENT"
        }
        """;

    given()
        .contentType("application/json")
        .body(json)
        .when()
        .patch("store/9999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteStore() {
    String json = """
        {
          "name": "STORE_FOR_DELETION",
          "quantityProductsInStock": 5
        }
        """;

    int id = given()
        .contentType("application/json")
        .body(json)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .path("id");

    given()
        .when()
        .delete("store/" + id)
        .then()
        .statusCode(204);

    given()
        .when()
        .delete("store/9999")
        .then()
        .statusCode(404);
  }
}
