package com.meridian.platform.bedrockadapter.api;

import com.meridian.platform.bedrockadapter.copybook.AccountRecord;
import com.meridian.platform.bedrockadapter.copybook.CustomerRecord;
import com.meridian.platform.bedrockadapter.copybook.OnlineMessages.PostingResult;
import com.meridian.platform.bedrockadapter.copybook.TransactionRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST face of the adapter. Consumers: bff-retail, bff-business, txn-posting-service,
 * statements-api. Nothing here knows about copybooks; that is BedrockService.
 */
@RestController
@RequestMapping("/bedrock/v1")
public class AccountsController {

    private final BedrockService service;

    public AccountsController(BedrockService service) {
        this.service = service;
    }

    @GetMapping("/accounts/{accountId}")
    public AccountRecord account(@PathVariable String accountId,
                                 @RequestHeader(value = "X-Channel", defaultValue = "RTL") String channel,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return service.inquireAccount(accountId, channel, asOf);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionRecord> transactions(@PathVariable String accountId,
                                                @RequestParam(defaultValue = "50") int limit) {
        return service.transactions(accountId, limit);
    }

    @GetMapping("/customers/{customerId}/accounts")
    public List<AccountRecord> customerAccounts(@PathVariable String customerId) {
        return service.accountsForCustomer(customerId);
    }

    @GetMapping("/customers/{customerId}")
    public CustomerRecord customer(@PathVariable String customerId,
                                   @RequestHeader(value = "X-Channel", defaultValue = "RTL") String channel,
                                   @RequestParam(defaultValue = "NAME") String scope) {
        return service.customerProfile(customerId, channel, scope);
    }

    @PostMapping("/postings")
    public ResponseEntity<PostingResult> post(@Valid @RequestBody PostingRequest body,
                                              @RequestHeader(value = "X-Channel", defaultValue = "RTL") String channel) {
        PostingResult result = service.post(body, channel);
        HttpStatus status = "20".equals(result.getHeader().getReturnCode()) ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.status();
    }
}
