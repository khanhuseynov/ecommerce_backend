package com.kahnhuseynov.ecommercebackend.promotion.controller;

import com.kahnhuseynov.ecommercebackend.promotion.dto.*;
import com.kahnhuseynov.ecommercebackend.promotion.service.PromotionService;
import com.kahnhuseynov.ecommercebackend.core.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotionService;
    @PostMapping
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
    }
    @PutMapping("/{id}")
    public PromotionResponse update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
        return promotionService.update(id, request);
    }
    @GetMapping("/{id}")
    public PromotionResponse get(@PathVariable Long id) { return promotionService.get(id); }
    @GetMapping
    public PageResponse<PromotionResponse> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return PageResponse.from(promotionService.getAll(pageable));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        promotionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
