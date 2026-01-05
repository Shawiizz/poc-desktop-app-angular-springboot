package sample.app.desktop;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class DesktopApplicationTests {

    @Test
    void testHelloEndpoint() {
        given()
            .contentType("text/plain")
            .body("Test message")
            .when().post("/api/hello")
            .then()
            .statusCode(200)
            .body(is("Hello from Quarkus!"));
    }

    @Test
    void testLogsPathEndpoint() {
        given()
            .when().get("/api/logs/path")
            .then()
            .statusCode(200);
    }
}
