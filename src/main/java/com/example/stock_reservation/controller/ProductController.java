package com.example.stock_reservation.controller;

import com.example.stock_reservation.entity.Product;
import com.example.stock_reservation.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> Map.<String, Object>of(
                        "id", product.getId(),
                        "sku", product.getSku(),
                        "name", product.getName(),
                        "totalStock", product.getTotalStock(),
                        "reservedStock", product.getReservedStock(),
                        "availableStock",
                        product.getTotalStock() - product.getReservedStock()
                ))
                .toList();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        product.setReservedStock(0);
        return productRepository.save(product);
    }
}