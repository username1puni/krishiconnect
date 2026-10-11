package com.krishiconnect.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.dto.QuoteRequest;
import com.krishiconnect.dto.QuoteResponse;
import com.krishiconnect.dto.RequirementRequest;
import com.krishiconnect.dto.RequirementResponse;
import com.krishiconnect.entity.ResourceType;
import com.krishiconnect.security.AuthUser;
import com.krishiconnect.service.QuoteService;
import com.krishiconnect.service.RequirementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Requirements & quotes")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;
    private final QuoteService quoteService;

    @GetMapping("/requirements")
    public PageResponse<RequirementResponse> board(@RequestParam(required = false) Double lat,
                                                   @RequestParam(required = false) Double lng,
                                                   @RequestParam(required = false) Double radiusKm,
                                                   @RequestParam(required = false) ResourceType needType,
                                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                                   @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return requirementService.board(lat, lng, radiusKm, needType, page, size);
    }

    @PostMapping("/requirements")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<RequirementResponse> create(@AuthenticationPrincipal AuthUser me,
                                                      @Valid @RequestBody RequirementRequest request) {
        RequirementResponse created = requirementService.create(me.getId(), request);
        return ResponseEntity.created(URI.create("/api/requirements/" + created.id())).body(created);
    }

    @GetMapping("/requirements/mine")
    @PreAuthorize("hasRole('SEEKER')")
    public List<RequirementResponse> mine(@AuthenticationPrincipal AuthUser me) {
        return requirementService.mine(me.getId());
    }

    @GetMapping("/requirements/{id}")
    public RequirementResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return requirementService.get(id, me);
    }

    @PostMapping("/requirements/{id}/close")
    @PreAuthorize("hasRole('SEEKER')")
    public RequirementResponse close(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return requirementService.close(me.getId(), id);
    }

    @PostMapping("/requirements/{id}/quotes")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SPECIALIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse quote(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                               @Valid @RequestBody QuoteRequest request) {
        return quoteService.submit(me.getId(), id, request);
    }

    @Operation(summary = "Accept one quote. Creates a confirmed booking and turns down the other quotes.")
    @PostMapping("/requirements/{id}/quotes/{quoteId}/accept")
    @PreAuthorize("hasRole('SEEKER')")
    public QuoteResponse accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                @PathVariable Long quoteId) {
        return quoteService.accept(me.getId(), id, quoteId);
    }

    @GetMapping("/quotes/mine")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SPECIALIST')")
    public List<QuoteResponse> myQuotes(@AuthenticationPrincipal AuthUser me) {
        return quoteService.mine(me.getId());
    }

    @PostMapping("/quotes/{quoteId}/withdraw")
    @PreAuthorize("hasAnyRole('PROVIDER', 'SPECIALIST')")
    public QuoteResponse withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable Long quoteId) {
        return quoteService.withdraw(me.getId(), quoteId);
    }
}
