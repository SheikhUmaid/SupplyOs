package com.supplychain.category;

import com.supplychain.category.CategoryDtos.CategoryRequest;
import com.supplychain.category.CategoryDtos.CategoryResponse;
import com.supplychain.exception.ApiException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        return save(new Category(request.name().trim(), request.description()));
    }

    @Transactional
    public CategoryResponse update(long id, CategoryRequest request) {
        Category category = categories.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This category doesn't exist."));
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return save(category);
    }

    private CategoryResponse save(Category category) {
        try {
            return CategoryResponse.from(categories.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "A category with this name already exists.");
        }
    }
}
