package com.example.sapir.webstore.service;

import com.example.sapir.webstore.entity.Product;
import com.example.sapir.webstore.repository.ProductRepository;
import com.example.sapir.webstore.repository.CategoryRepository;
import com.example.sapir.webstore.entity.Category;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return repository.findByCategory_Id(categoryId);
    }

    public List<Product> getUncategorizedProducts() {
        return repository.findByCategoryIsNull();
    }

    public Product getProductById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    public Product createProduct(Product product) {
        product.setId(null);
        product.setCategory(resolveCategory(product.getCategory()));
        return repository.save(product);
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        Product product = getProductById(id);
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStockQuantity(updatedProduct.getStockQuantity());
        product.setCategory(resolveCategory(updatedProduct.getCategory()));
        return repository.save(product);
    }

    private Category resolveCategory(Category requestedCategory) {
        if (requestedCategory == null || requestedCategory.getId() == null) {
            return null;
        }
        return categoryRepository.findById(requestedCategory.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found: " + requestedCategory.getId()));
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        repository.delete(product);
    }
}
