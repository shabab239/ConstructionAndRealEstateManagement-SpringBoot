// src/main/java/com/shabab/RealEstateManagementSystem/core/service/RawMaterialService.java

package com.shabab.RealEstateManagementSystem.core.service;

import com.shabab.RealEstateManagementSystem.account.model.Account;
import com.shabab.RealEstateManagementSystem.account.model.Transaction;
import com.shabab.RealEstateManagementSystem.account.repository.AccountRepository;
import com.shabab.RealEstateManagementSystem.account.repository.TransactionRepository;
import com.shabab.RealEstateManagementSystem.core.model.RawMaterial;
import com.shabab.RealEstateManagementSystem.core.model.RawMaterialOrder;
import com.shabab.RealEstateManagementSystem.core.model.RawMaterialStock;
import com.shabab.RealEstateManagementSystem.core.model.RawMaterialUsage;
import com.shabab.RealEstateManagementSystem.core.repository.*;
import com.shabab.RealEstateManagementSystem.util.ApiResponse;
import com.shabab.RealEstateManagementSystem.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Project: ConstructionAndRealEstateManagement-SpringBoot
 * Author: Shabab
 * Created on: 30/09/2024
 */

@Service
public class RawMaterialService {

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private RawMaterialOrderRepository rawMaterialOrderRepository;

    @Autowired
    private RawMaterialStockRepository rawMaterialStockRepository;
    @Autowired
    private RawMaterialUsageRepository rawMaterialUsageRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    // RawMaterial methods
    public ApiResponse getById(Long id) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterial rawMaterial = rawMaterialRepository.findByIdAndCompanyId(
                    id, AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (rawMaterial == null) {
                return response.error("Raw material not found");
            }
            response.setData("rawMaterial", rawMaterial);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved raw material");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse getAll() {
        ApiResponse response = new ApiResponse();
        try {
            List<RawMaterial> rawMaterials = rawMaterialRepository.findAllByCompanyId(
                    AuthUtil.getCurrentCompanyId()
            ).orElse(new ArrayList<>());
            response.setData("rawMaterials", rawMaterials);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved raw materials");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse save(RawMaterial rawMaterial) {
        ApiResponse response = new ApiResponse();
        try {
            rawMaterial.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialRepository.save(rawMaterial);
            response.setData("rawMaterial", rawMaterial);
            response.setSuccessful(true);
            response.success("Saved Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse update(RawMaterial rawMaterial) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterial dbRawMaterial = rawMaterialRepository.findByIdAndCompanyId(
                    rawMaterial.getId(), AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (dbRawMaterial == null) {
                return response.error("Raw material not found");
            }
            rawMaterial.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialRepository.save(rawMaterial);
            response.setData("rawMaterial", rawMaterial);
            response.setSuccessful(true);
            response.success("Updated Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse deleteById(Long id) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterial dbRawMaterial = rawMaterialRepository.findByIdAndCompanyId(
                    id, AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (dbRawMaterial == null) {
                return response.error("Raw material not found");
            }
            rawMaterialRepository.delete(dbRawMaterial);
            response.setSuccessful(true);
            response.success("Deleted Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    // RawMaterialOrder methods
    public ApiResponse getAllOrders() {
        ApiResponse response = new ApiResponse();
        try {
            List<RawMaterialOrder> orders = rawMaterialOrderRepository.findAllByCompanyId(
                    AuthUtil.getCurrentCompanyId()
            ).orElse(new ArrayList<>());
            response.setData("orders", orders);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved orders");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    @Transactional(rollbackOn = Exception.class)
    public ApiResponse saveOrder(RawMaterialOrder rawMaterialOrder) {
        ApiResponse response = new ApiResponse();
        try {
            if (rawMaterialOrder.getStatus().equals(RawMaterialOrder.RawMaterialOrderStatus.DELIVERED)) {
                RawMaterialStock stock = rawMaterialStockRepository.findByRawMaterialIdAndCompanyId(
                        rawMaterialOrder.getRawMaterial().getId(), AuthUtil.getCurrentCompanyId()
                ).orElse(null);
                if (stock == null) {
                    stock = new RawMaterialStock();
                    stock.setRawMaterial(rawMaterialOrder.getRawMaterial());
                    stock.setQuantity(rawMaterialOrder.getQuantity());
                } else {
                    stock.setQuantity(stock.getQuantity() + rawMaterialOrder.getQuantity());
                }
                stock.setLastUpdated(new Date());
                stock.setCompanyId(AuthUtil.getCurrentCompanyId());
                rawMaterialStockRepository.save(stock);

                String groupTransactionId = UUID.randomUUID().toString();

                Transaction debitTransaction = new Transaction();
                debitTransaction.setAmount(rawMaterialOrder.getTotalPrice());
                debitTransaction.setType(Transaction.TransactionType.DEBIT);
                debitTransaction.setAccount(rawMaterialOrder.getSupplier().getAccount());
                debitTransaction.setGroupTransactionId(groupTransactionId);
                debitTransaction.setTransactionDate(new Date());
                debitTransaction.setParticular("Raw Material Order - " + rawMaterialOrder.getRawMaterial().getName());
                debitTransaction.setCompanyId(AuthUtil.getCurrentCompanyId());
                transactionRepository.save(debitTransaction);

                Transaction creditTransaction = new Transaction();
                creditTransaction.setAmount(rawMaterialOrder.getTotalPrice());
                creditTransaction.setType(Transaction.TransactionType.CREDIT);
                creditTransaction.setAccount(AuthUtil.getCurrentUser().getAccount());
                creditTransaction.setGroupTransactionId(groupTransactionId);
                creditTransaction.setTransactionDate(new Date());
                creditTransaction.setParticular("Raw Material Order - " + rawMaterialOrder.getRawMaterial().getName());
                creditTransaction.setCompanyId(AuthUtil.getCurrentCompanyId());
                transactionRepository.save(creditTransaction);

                rawMaterialOrder.setGroupTransactionId(groupTransactionId);

                Account supplierAccount = rawMaterialOrder.getSupplier().getAccount();
                supplierAccount.setBalance(supplierAccount.getBalance() + rawMaterialOrder.getTotalPrice());
                Account companyAccount = AuthUtil.getCurrentUser().getAccount();
                debitTransaction.setParticular("Raw Material Order - " + rawMaterialOrder.getRawMaterial().getName());
                companyAccount.setBalance(companyAccount.getBalance() - rawMaterialOrder.getTotalPrice());
                accountRepository.save(supplierAccount);
                accountRepository.save(companyAccount);

            }
            rawMaterialOrder.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialOrderRepository.save(rawMaterialOrder);
            response.setData("order", rawMaterialOrder);
            response.setSuccessful(true);
            response.success("Saved Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    @Transactional(rollbackOn = Exception.class)
    public ApiResponse updateOrder(RawMaterialOrder rawMaterialOrder) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialOrder dbOrder = rawMaterialOrderRepository.findByIdAndCompanyId(
                    rawMaterialOrder.getId(), AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (dbOrder == null) {
                return response.error("Order not found");
            }
            if (rawMaterialOrder.getStatus().equals(RawMaterialOrder.RawMaterialOrderStatus.DELIVERED)) {
                RawMaterialStock stock = rawMaterialStockRepository.findByRawMaterialIdAndCompanyId(
                        rawMaterialOrder.getRawMaterial().getId(), AuthUtil.getCurrentCompanyId()
                ).orElse(null);
                if (stock == null) {
                    stock = new RawMaterialStock();
                    stock.setRawMaterial(rawMaterialOrder.getRawMaterial());
                    stock.setQuantity(rawMaterialOrder.getQuantity());
                } else {
                    stock.setQuantity(stock.getQuantity() + rawMaterialOrder.getQuantity());
                }
                stock.setLastUpdated(new Date());
                stock.setCompanyId(AuthUtil.getCurrentCompanyId());
                rawMaterialStockRepository.save(stock);

                if (rawMaterialOrder.getGroupTransactionId() != null) {
                    Transaction debitTransaction = transactionRepository.findByGroupTransactionIdAndTypeAndCompanyId(
                            rawMaterialOrder.getGroupTransactionId(), Transaction.TransactionType.DEBIT, AuthUtil.getCurrentCompanyId()
                    ).orElse(null);
                    if (debitTransaction != null) {
                        debitTransaction.setAmount(rawMaterialOrder.getTotalPrice());
                        transactionRepository.save(debitTransaction);
                    }

                    Transaction creditTransaction = transactionRepository.findByGroupTransactionIdAndTypeAndCompanyId(
                            rawMaterialOrder.getGroupTransactionId(), Transaction.TransactionType.CREDIT, AuthUtil.getCurrentCompanyId()
                    ).orElse(null);
                    if (creditTransaction != null) {
                        creditTransaction.setAmount(rawMaterialOrder.getTotalPrice());
                        transactionRepository.save(creditTransaction);
                    }
                } else {
                    String groupTransactionId = UUID.randomUUID().toString();

                    Transaction debitTransaction = new Transaction();
                    debitTransaction.setAmount(rawMaterialOrder.getTotalPrice());
                    debitTransaction.setType(Transaction.TransactionType.DEBIT);
                    debitTransaction.setAccount(rawMaterialOrder.getSupplier().getAccount());
                    debitTransaction.setGroupTransactionId(groupTransactionId);
                    debitTransaction.setTransactionDate(new Date());
                    debitTransaction.setParticular("Raw Material Order - " + rawMaterialOrder.getRawMaterial().getName());
                    debitTransaction.setCompanyId(AuthUtil.getCurrentCompanyId());
                    transactionRepository.save(debitTransaction);

                    Transaction creditTransaction = new Transaction();
                    creditTransaction.setAmount(rawMaterialOrder.getTotalPrice());
                    creditTransaction.setType(Transaction.TransactionType.CREDIT);
                    creditTransaction.setAccount(AuthUtil.getCurrentUser().getAccount());
                    creditTransaction.setGroupTransactionId(groupTransactionId);
                    creditTransaction.setTransactionDate(new Date());
                    creditTransaction.setParticular("Raw Material Order - " + rawMaterialOrder.getRawMaterial().getName());
                    creditTransaction.setCompanyId(AuthUtil.getCurrentCompanyId());
                    transactionRepository.save(creditTransaction);

                    rawMaterialOrder.setGroupTransactionId(groupTransactionId);

                    Account supplierAccount = rawMaterialOrder.getSupplier().getAccount();
                    supplierAccount.setBalance(supplierAccount.getBalance() + rawMaterialOrder.getTotalPrice());
                    Account companyAccount = AuthUtil.getCurrentUser().getAccount();
                    companyAccount.setBalance(companyAccount.getBalance() - rawMaterialOrder.getTotalPrice());
                    accountRepository.save(supplierAccount);
                    accountRepository.save(companyAccount);
                }
            }
            rawMaterialOrder.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialOrderRepository.save(rawMaterialOrder);
            response.setData("order", rawMaterialOrder);
            response.setSuccessful(true);
            response.success("Updated Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse getOrderById(Long id) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialOrder order = rawMaterialOrderRepository.findByIdAndCompanyId(
                    id, AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (order == null) {
                return response.error("Order not found");
            }
            response.setData("order", order);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved order");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse deleteOrderById(Long id) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialOrder dbOrder = rawMaterialOrderRepository.findByIdAndCompanyId(
                    id, AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (dbOrder == null) {
                return response.error("Order not found");
            }
            rawMaterialOrderRepository.delete(dbOrder);
            response.setSuccessful(true);
            response.success("Deleted Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    // RawMaterialStock methods
    public ApiResponse getAllStocks() {
        ApiResponse response = new ApiResponse();
        try {
            List<RawMaterialStock> stocks = rawMaterialStockRepository.findAllByCompanyId(
                    AuthUtil.getCurrentCompanyId()
            ).orElse(new ArrayList<>());
            response.setData("stocks", stocks);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved stocks");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse saveStock(RawMaterialStock rawMaterialStock) {
        ApiResponse response = new ApiResponse();
        try {
            rawMaterialStock.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialStockRepository.save(rawMaterialStock);
            response.setData("stock", rawMaterialStock);
            response.setSuccessful(true);
            response.success("Saved Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse updateStock(RawMaterialStock rawMaterialStock) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialStock dbStock = rawMaterialStockRepository.findByIdAndCompanyId(
                    rawMaterialStock.getId(), AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (dbStock == null) {
                return response.error("Stock not found");
            }
            rawMaterialStock.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialStockRepository.save(rawMaterialStock);
            response.setData("stock", rawMaterialStock);
            response.setSuccessful(true);
            response.success("Updated Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse getStockById(Long id) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialStock stock = rawMaterialStockRepository.findByIdAndCompanyId(
                    id, AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (stock == null) {
                return response.error("Stock not found");
            }
            response.setData("stock", stock);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved stock");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse deleteStockById(Long id) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialStock dbStock = rawMaterialStockRepository.findByIdAndCompanyId(
                    id, AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (dbStock == null) {
                return response.error("Stock not found");
            }
            rawMaterialStockRepository.delete(dbStock);
            response.setSuccessful(true);
            response.success("Deleted Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse getAllUsages() {
        ApiResponse response = new ApiResponse();
        try {
            List<RawMaterialUsage> usages = rawMaterialUsageRepository.findAllByCompanyId(
                    AuthUtil.getCurrentCompanyId()
            ).orElse(new ArrayList<>());
            response.setData("usages", usages);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved usages");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    public ApiResponse getAllUsagesByStageId(Long stageId) {
        ApiResponse response = new ApiResponse();
        try {
            List<RawMaterialUsage> usages = rawMaterialUsageRepository.findAllByStageIdAndCompanyId(
                    stageId, AuthUtil.getCurrentCompanyId()
            ).orElse(new ArrayList<>());
            response.setData("usages", usages);
            response.setSuccessful(true);
            response.setMessage("Successfully retrieved usages");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }

    @Transactional(rollbackOn = Exception.class)
    public ApiResponse saveRawMaterialUsage(RawMaterialUsage rawMaterialUsage) {
        ApiResponse response = new ApiResponse();
        try {
            RawMaterialStock stock = rawMaterialStockRepository.findByRawMaterialIdAndCompanyId(
                    rawMaterialUsage.getRawMaterial().getId(), AuthUtil.getCurrentCompanyId()
            ).orElse(null);
            if (stock == null) {
                return response.error("Stock not found");
            }
            if (stock.getQuantity() < rawMaterialUsage.getQuantity()) {
                return response.error("Not enough stock");
            }
            stock.setQuantity(stock.getQuantity() - rawMaterialUsage.getQuantity());
            stock.setLastUpdated(new Date());
            stock.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialStockRepository.save(stock);

            rawMaterialUsage.setCompanyId(AuthUtil.getCurrentCompanyId());
            rawMaterialUsageRepository.save(rawMaterialUsage);
            response.setData("usage", rawMaterialUsage);
            response.setSuccessful(true);
            response.success("Saved Successfully");
        } catch (Exception e) {
            return response.error(e);
        }
        return response;
    }


}