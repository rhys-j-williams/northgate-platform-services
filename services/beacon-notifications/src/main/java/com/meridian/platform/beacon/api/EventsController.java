package com.meridian.platform.beacon.api;

import com.meridian.platform.beacon.event.AccountEvent;
import com.meridian.platform.beacon.event.EventIngestService;
import java.util.Collections;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Side door for injecting events over HTTP. Exists for the smoke test and for Ops replaying an
 * event from the Bedrock bridge log. Production traffic arrives over MQ only.
 */
@RestController
@RequestMapping("/beacon/v1/events")
public class EventsController {

    private final EventIngestService ingest;

    public EventsController(EventIngestService ingest) {
        this.ingest = ingest;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> inject(@Valid @RequestBody AccountEvent event) {
        int released = ingest.accept(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(Collections.singletonMap("released", released));
    }
}
