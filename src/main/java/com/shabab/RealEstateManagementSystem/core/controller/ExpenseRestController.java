package com.shabab.RealEstateManagementSystem.core.controller;

import com.shabab.RealEstateManagementSystem.core.model.PaymentSchedule;
import com.shabab.RealEstateManagementSystem.core.service.ExpenseService;
import com.shabab.RealEstateManagementSystem.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Project: ConstructionAndRealEstateManagement-SpringBoot
 * Author: Shabab
 * Created on: 28/09/2024
 */

@CrossOrigin
@RestController
@RequestMapping("/api/expense")
public class ExpenseRestController {

    @Autowired
    private ExpenseService expenseService;

    @GetMapping("/")
    public ApiResponse findAll() {
        return expenseService.findAll();
    }

    @PostMapping("/save")
    public ApiResponse save(@Valid @RequestBody PaymentSchedule paymentSchedule) {
        return expenseService.save(paymentSchedule);
    }

    @PutMapping("/update")
    public ApiResponse update(@Valid @RequestBody PaymentSchedule paymentSchedule) {
        return expenseService.update(paymentSchedule);
    }

    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        return expenseService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteById(@PathVariable Long id) {
        return expenseService.deleteById(id);
    }
}