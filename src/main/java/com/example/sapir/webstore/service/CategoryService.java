package com.example.sapir.webstore.service;

import com.example.sapir.webstore.entity.Category;
import com.example.sapir.webstore.entity.Product;
import com.example.sapir.webstore.repository.CategoryRepository;
import com.example.sapir.webstore.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(Category category) {
        category.setId(null);
        if (categoryRepository.existsByNameIgnoreCase(category.getName().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists.");
        }
        category.setName(category.getName().trim());
        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category updatedCategory) {
        Category category = getCategory(id);
        String name = updatedCategory.getName().trim();
        boolean duplicate = categoryRepository.existsByNameIgnoreCase(name)
                && !category.getName().equalsIgnoreCase(name);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A category with this name already exists.");
        }
        category.setName(name);
        category.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategory(id);
        List<Product> products = productRepository.findByCategory_Id(id);
        products.forEach(product -> product.setCategory(null));
        productRepository.saveAll(products);
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + id));
    }
}
