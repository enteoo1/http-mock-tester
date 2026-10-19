package com.example.mocktester.mock;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/** Catches every HTTP method on /mock/{username}/{context}/{scenario}[/anything]. */
@RestController
public class MockEndpointController {

    private final MockService service;

    public MockEndpointController(MockService service) {
        this.service = service;
    }

    @RequestMapping({"/mock/{username}/{context}/{scenario}", "/mock/{username}/{context}/{scenario}/**"})
    public ResponseEntity<byte[]> handle(@PathVariable String username, @PathVariable String context,
                                         @PathVariable String scenario, HttpServletRequest request)
            throws IOException {
        MockResponse r = service.process(username, context, scenario, request);
        HttpHeaders headers = new HttpHeaders();
        r.headers().forEach(headers::add);
        ResponseEntity.BodyBuilder b = ResponseEntity.status(r.status()).headers(headers);
        return r.body() == null ? b.build() : b.body(r.body());
    }
}
